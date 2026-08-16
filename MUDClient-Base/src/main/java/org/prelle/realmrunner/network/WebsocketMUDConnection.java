package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import org.prelle.mudevents.BinaryDataEvent;
import org.prelle.mudevents.MUDEvent;

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
	
	private final static Logger logger = System.getLogger("mud.client.ws");
	
	public static class MyWebSocketClient extends Endpoint {
	    @Override
	    public void onOpen(Session session, EndpointConfig config) {
	      logger.log(Level.INFO, "onOpen: Connected to server: " + session.getNegotiatedSubprotocol());
	        // You can send messages to the server using session.getBasicRemote().sendText("message");
	    }


	    @OnMessage
	    public void onMessage(String message) {
	        System.out.println("Received message: " + message);
	    }
	    @OnMessage
	    public void onBinaryMessage(byte[] message) {
	        logger.log(Level.WARNING,"Received binary message of length: " + message.length);
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
	public WebsocketMUDConnection(URI uri) throws IOException {
		this.host = InetAddress.getByName(uri.getHost());
		this.port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("wss") ? 443 : 80);
		
		MessageHandler binaryHandler = new MessageHandler.Whole<byte[]>() {
		    @Override
		    public void onMessage(byte[] message) {
		        getReceivePipe().publish(new BinaryDataEvent(this,message));
		    }
		};
		
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.preferredSubprotocols(List.of("telnet.mudstandards.org","terminal.mudstandards.org","muddown"))
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
	        	session.addMessageHandler(binaryHandler);
	        	negotiatedSubprotocol = session.getNegotiatedSubprotocol();
	            System.out.println("Connected to server: "+session.getNegotiatedSubprotocol());
	            if (negotiatedSubprotocol == null || "telnet.mudstandards.org".equals(negotiatedSubprotocol) ) {
	            	super.supportsTelnet = true;
	            } else if ("muddown".equals(negotiatedSubprotocol)) {
	            	super.supportsMUDDown = true;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
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
