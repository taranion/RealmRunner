package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;

import org.prelle.ansi.PrintableFragment;
import org.prelle.muddown.MessageEnvelope;
import org.prelle.mudevents.BinaryDataEvent;
import org.prelle.mudevents.MUDEvent;
import org.prelle.mudevents.ansi.ANSIEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import lombok.Getter;

/**
 * 
 */
public class WebsocketMUDConnection extends MUDConnection {
	
	public final static String SUBPROTOCOL_TELNET = "telnet.mudstandards.org";
	public final static String SUBPROTOCOL_TELNETB64 = "telnetb64.mudstandards.org";
	public final static String SUBPROTOCOL_TERMINAL = "terminal.mudstandards.org";
	public final static String SUBPROTOCOL_MUDDOWN = "muddown";
	
	private final static Logger logger = System.getLogger("mud.client.ws");
	
	private final static Gson gson;
	
    static {
        class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
            @Override
            public void write(final JsonWriter out, final LocalDateTime value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    out.value(value.toString());
                }
            }

            @Override
            public LocalDateTime read(final JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                String text = in.nextString();
                try {
                    return Instant.parse(text).atZone(ZoneOffset.UTC).toLocalDateTime();
                } catch (DateTimeParseException e) {
                    return LocalDateTime.parse(text);
                }
            }
        }

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

	
	
	public static class MyWebSocketClient extends Endpoint {
	    @Override
	    public void onOpen(Session session, EndpointConfig config) {
	      logger.log(Level.INFO, "onOpen: Connected to server: {0} using {1}", session.getRequestURI(), session.getNegotiatedSubprotocol());
	        // You can send messages to the server using session.getBasicRemote().sendText("message");
	    }


	    @OnMessage
	    public void onBinaryMessage(Session session, byte[] message) {
	        logger.log(Level.WARNING,"Received binary message of length: " + message.length);
	        //incomingData.receiveData(message);
	    }
	    @OnMessage
	    public void onTextMessage(Session session, String message) {
	        logger.log(Level.WARNING,"Received text message of length: " + message);
	        //incomingData.receiveData(message);
	    }
	    @Override
	    public void onClose(Session session, CloseReason closeReason) {
	        System.out.println("WebSocket connection closed: " + closeReason);
	    }

	    @Override
	    public void onError(Session session, Throwable thr) {
	        System.err.println("WebSocket error: " + thr.getMessage());
	    }
	}
	
	
	@Getter private InetAddress host;
	@Getter private int port;
	@Getter private String negotiatedSubprotocol;
	

	//-------------------------------------------------------------------
	/**
	 */
	public WebsocketMUDConnection(URI uri, String subprotocol) throws IOException {
		this.host = InetAddress.getByName(uri.getHost());
		this.port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("wss") ? 443 : 80);
		
		MessageHandler binaryHandler = new MessageHandler.Whole<byte[]>() {
		    @Override
		    public void onMessage(byte[] message) {
		        getReceivePipe().publish(new BinaryDataEvent(this,message));
		    }
		};
		MessageHandler textHandler = new MessageHandler.Whole<String>() {
		    @Override
		    public void onMessage(String message) {
		    	logger.log(Level.INFO,"Received text message: " +supportsTelnet+" ="+ message);
		    	if (supportsTelnet) {
		    		byte[] buf = Base64.getDecoder().decode(message);
			        getReceivePipe().publish(new BinaryDataEvent(this,buf));
		    	} else if (supportsMUDDown) {
					var mudDownMsg = gson.fromJson(message, MessageEnvelope.class);
					getReceivePipe().publish(mudDownMsg);
		    	} else {
		    		// Send as ANSI Event with a PrintableDataFragment
		    		getReceivePipe().publish(new ANSIEvent(this, new PrintableFragment(message)));
		    		
		    	}
		        //getReceivePipe().publish(new BinaryDataEvent(this,message));
		    }
		};
		
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.preferredSubprotocols(List.of(SUBPROTOCOL_MUDDOWN,SUBPROTOCOL_TELNET,SUBPROTOCOL_TERMINAL))
				.configurator(new ClientEndpointConfig.Configurator() {
					@Override
					public void beforeRequest(java.util.Map<String, java.util.List<String>> headers) {
						headers.put("Origin", List.of("http://"+host.getHostName()+":"+port));
					}
				})
				.build();
		 WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	        try {
	        	Session session = container.connectToServer(MyWebSocketClient.class, config, uri);
	        	negotiatedSubprotocol = session.getNegotiatedSubprotocol();
	            System.out.println("Connected to server: "+session.getNegotiatedSubprotocol());
	        	session.addMessageHandler(binaryHandler);
	        	session.addMessageHandler(textHandler);
	            if (negotiatedSubprotocol == null || SUBPROTOCOL_TELNET.equals(negotiatedSubprotocol) ) {
	            	super.supportsTelnet = true;
	            } else if ("muddown".equals(negotiatedSubprotocol)) {
	            	super.supportsMUDDown = true;
	            } else if (negotiatedSubprotocol==null || negotiatedSubprotocol.isEmpty()) {
	            	// Check configured subprotocols
	        		this.supportsTelnet = (subprotocol != null && (
	        				subprotocol.equals(SUBPROTOCOL_TELNET) || subprotocol.equals(SUBPROTOCOL_TELNETB64)));
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	}

	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#getName()
	 */
	@Override
	public String getName() {
		return "Websocket";
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.MUDEvent)
	 */
	@Override
	public List<MUDEvent> apply(MUDEvent event) {
		if (event instanceof BinaryDataEvent binary) {
			logger.log(Level.INFO,"Send binary data of length: "+binary.getData().length);
			byte[] data = binary.getData();
			// ToDo: Send on websocket
		} else
			logger.log(Level.ERROR, "Unhandled send event type: "+event.getClass().getName());
		return List.of();
	}


	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
