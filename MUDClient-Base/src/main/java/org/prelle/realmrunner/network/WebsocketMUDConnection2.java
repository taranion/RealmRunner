package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URI;
import java.util.Base64;
import java.util.List;

import org.prelle.ansi.PrintableFragment;
import org.prelle.mudevents.BinaryDataEvent;
import org.prelle.mudevents.PipeEvent;
import org.prelle.mudevents.ansi.ANSIEvent;

import lombok.Getter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * WebSocket implementation using OkHttp. Supports redirects via OkHttp's followRedirects.
 */
public class WebsocketMUDConnection2 extends MUDConnection {

    public final static String SUBPROTOCOL_TELNET = "telnet.mudstandards.org";
    public final static String SUBPROTOCOL_TELNETB64 = "telnetb64.mudstandards.org";
    public final static String SUBPROTOCOL_TERMINAL = "terminal.mudstandards.org";
    public final static String SUBPROTOCOL_MUDDOWN = "muddown";

    private final static Logger logger = System.getLogger("mud.client.ws.okhttp");

    @Getter private InetAddress host;
    @Getter private int port;
    @Getter private String negotiatedSubprotocol;

    private final OkHttpClient client;
    private WebSocket webSocket;

    public WebsocketMUDConnection2(URI uri, String subprotocol) throws IOException {
        this.host = InetAddress.getByName(uri.getHost());
        this.port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("wss") ? 443 : 80);

        // Build OkHttpClient that follows redirects (both HTTP and HTTPS)
        this.client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        // Prepare WebSocket request
        Request.Builder rb = new Request.Builder().url(uri.toString())
                .header("Origin", "http://" + host.getHostName() + ":" + port);

        // Offer subprotocols: prefer provided subprotocol or fall back to common ones
        String protocols;
        if (subprotocol != null && !subprotocol.isEmpty()) {
            protocols = subprotocol;
        } else {
            protocols = String.join(",", SUBPROTOCOL_TELNET, SUBPROTOCOL_TERMINAL, SUBPROTOCOL_MUDDOWN);
        }
        rb.header("Sec-WebSocket-Protocol", protocols);

        Request request = rb.build();

        // Connect asynchronously; the returned WebSocket instance can be used to send messages immediately.
        this.webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                negotiatedSubprotocol = response.header("Sec-WebSocket-Protocol");
                logger.log(Level.INFO, "OkHttp WebSocket onOpen, negotiated subprotocol=" + negotiatedSubprotocol);
                if (negotiatedSubprotocol == null || SUBPROTOCOL_TELNET.equals(negotiatedSubprotocol)) {
                    WebsocketMUDConnection2.super.supportsTelnet = true;
                } else if ("muddown".equals(negotiatedSubprotocol)) {
                    WebsocketMUDConnection2.super.supportsMUDDown = true;
                } else if (negotiatedSubprotocol == null || negotiatedSubprotocol.isEmpty()) {
                    WebsocketMUDConnection2.super.supportsTelnet = (subprotocol != null && (
                            subprotocol.equals(SUBPROTOCOL_TELNET) || subprotocol.equals(SUBPROTOCOL_TELNETB64)));
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                logger.log(Level.INFO, "OkHttp received text message: " + text);
                if (WebsocketMUDConnection2.this.supportsTelnet) {
                    byte[] buf = Base64.getDecoder().decode(text);
                    getReceivePipe().publish(new BinaryDataEvent(buf));
                } else {
                    getReceivePipe().publish(new ANSIEvent(new PrintableFragment(text)));
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                logger.log(Level.INFO, "OkHttp received binary message of length: " + bytes.size());
                getReceivePipe().publish(new BinaryDataEvent(bytes.toByteArray()));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                logger.log(Level.INFO, "OkHttp websocket closing: " + code + " / " + reason);
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                logger.log(Level.INFO, "OkHttp websocket closed: " + code + " / " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                logger.log(Level.ERROR, "OkHttp websocket failure: " + t.getMessage());
            }
        });

        // Note: OkHttp's newWebSocket is asynchronous; listener will set negotiatedSubprotocol when opened.
    }

    @Override
    public String getName() {
        return "Websocket(OkHttp)";
    }

    @Override
    public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
        if (event instanceof BinaryDataEvent binary) {
            logger.log(Level.INFO, "Send binary data of length: " + binary.getData().length);
            byte[] data = binary.getData();
            if (webSocket != null) {
                webSocket.send(ByteString.of(data));
            } else {
                logger.log(Level.ERROR, "WebSocket not connected - cannot send data");
            }
        } else {
            logger.log(Level.ERROR, "Unhandled send event type: " + event.getClass().getName());
        }
        return List.of();
    }

    @Override
    public void start() {
        // OkHttp connection is started during construction (async). Nothing to do here.
    }

    @Override
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "client close");
            webSocket = null;
        }
        // shutdown dispatcher's executor to free resources
        try {
            client.dispatcher().executorService().shutdown();
        } catch (Exception e) {
            // ignore
        }
    }

}
