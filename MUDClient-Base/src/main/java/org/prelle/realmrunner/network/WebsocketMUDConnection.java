package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.prelle.telnet.ReceiveDatagramInputStream;
import org.prelle.telnet.SendDatagramOutputStream;

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
public class WebsocketMUDConnection implements MUDConnection {
	
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
	
	ReceiveDatagramInputStream incomingData;
	SendDatagramOutputStream outgoingData;

	//-------------------------------------------------------------------
	/**
	 */
	public WebsocketMUDConnection(InetAddress host, int port) {
		this.host = host;
		this.port = port;
		
		incomingData = new ReceiveDatagramInputStream();
		
		MessageHandler binaryHandler = new MessageHandler.Whole<byte[]>() {
		    @Override
		    public void onMessage(byte[] message) {

		        incomingData.receiveData(message);
		    }
		};
		
		ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
				.preferredSubprotocols(List.of("telnet.mudstandards.org","terminal.mudstandards.org"))
				.configurator(new ClientEndpointConfig.Configurator() {
					@Override
					public void beforeRequest(java.util.Map<String, java.util.List<String>> headers) {
						headers.put("Origin", List.of("http://"+host.getHostName()+":"+port));
					}
				})
				.build();
		 WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	        String uri = "ws://"+host.getHostName()+":"+port; // Example WebSocket server
	        try {
	        	Session session = container.connectToServer(MyWebSocketClient.class, config, URI.create(uri));
	        	session.addMessageHandler(binaryHandler);
	    		outgoingData = new SendDatagramOutputStream( buf -> {
					try {
						session.getBasicRemote().sendBinary(ByteBuffer.wrap(buf));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
	            System.out.println("Connected to server: "+session.getNegotiatedSubprotocol());
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDConnection#getStreamFromMUD()
	 */
	@Override
	public InputStream getStreamFromMUD() {
		return incomingData;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDConnection#getStreamToMUD()
	 */
	@Override
	public OutputStream getStreamToMUD() {
		return outgoingData;
	}

}

//class ProcessIncomingData extends InputStream {
//	
//	private final static Logger logger = System.getLogger("mud.client.ws");
//	
//	private List<byte[]> incomingData = new ArrayList<>();
//	
//	private byte[] currentlyConsuming;
//	private int currentIndex = 0;
//
//	@Override
//	public int available() throws IOException {
//		logger.log(Level.INFO, "available: "+incomingData.size()+" currently: "+(currentlyConsuming != null ? currentlyConsuming.length : 0));
//		if (currentlyConsuming != null && currentIndex < currentlyConsuming.length) {
//			return currentlyConsuming.length - currentIndex;
//		}
//		synchronized (incomingData) {
//			int totalAvailable = 0;
//			for (byte[] data : incomingData) {
//				totalAvailable += data.length;
//			}
//			return totalAvailable;
//		}
//	}
//	
//	@Override
//	public int read() throws IOException {
//		logger.log(Level.WARNING, "readSingle");
//		if (currentlyConsuming != null && currentIndex < currentlyConsuming.length) {
//			return currentlyConsuming[currentIndex++] & 0xFF; // Return the next byte as an int
//		}
//		
//		synchronized (incomingData) {
//			while (incomingData.isEmpty()) {
//				try {
//					incomingData.wait(); // Wait for new data to arrive
//				} catch (InterruptedException e) {
//					Thread.currentThread().interrupt();
//					throw new IOException("Thread interrupted while waiting for data", e);
//				}
//			}
//			byte[] data = incomingData.remove(0);
//			currentlyConsuming = data;
//			currentIndex = 0;
//			return currentlyConsuming[currentIndex++] & 0xFF; // Return the next byte as an int
//		}
//	}
//	
//	public int read(byte[] b, int off, int len) throws IOException {
//		logger.log(Level.WARNING, "readMulti");
//		if (currentlyConsuming != null && currentIndex < currentlyConsuming.length) {
//			int bytesToRead = Math.min(len, currentlyConsuming.length - currentIndex);
//			System.arraycopy(currentlyConsuming, currentIndex, b, off, bytesToRead);
//			currentIndex += bytesToRead;
//			return bytesToRead;
//		}
//		
//		synchronized (incomingData) {
//			while (incomingData.isEmpty()) {
//				try {
//					incomingData.wait(); // Wait for new data to arrive
//				} catch (InterruptedException e) {
//					Thread.currentThread().interrupt();
//					throw new IOException("Thread interrupted while waiting for data", e);
//				}
//			}
//			byte[] data = incomingData.remove(0);
//			currentlyConsuming = data;
//			currentIndex = 0;
//			int bytesToRead = Math.min(len, currentlyConsuming.length - currentIndex);
//			System.arraycopy(currentlyConsuming, currentIndex, b, off, bytesToRead);
//			currentIndex += bytesToRead;
//			return bytesToRead;
//		}
//	}
//
//	@Override
//	public int read(byte[] data) throws IOException {
//		return read(data, 0, data.length);
//	}
//	
//	
//	void receiveData(byte[] data) {
//        logger.log(Level.WARNING,"Received binary message of length: " + data.length);
//        synchronized (incomingData) {
//			if (currentlyConsuming == null) {
//				currentlyConsuming = data;
//				currentIndex = 0;
//			} else {
//				incomingData.add(data);
//			}
//			incomingData.notifyAll(); // Notify any waiting threads that new data is available
//		}
//	}	
//}
//
//
//class SendOutgoingData extends OutputStream {
//	
//	private final static Logger logger = System.getLogger("mud.client.ws");
//	
//	private Session session;
//	
//	public SendOutgoingData(Session session) {
//		this.session = session;
//		// TODO Auto-generated constructor stub
//	}
//
//	@Override
//	public void write(int b) throws IOException {
//		logger.log(Level.WARNING, "writeSingle");
////		session.getBasicRemote().sendBinary(new ByteBuffer(new byte[] {(byte)b}));
//	}
//	
//	@Override
//	public void write(byte[] buf) throws IOException {
//		logger.log(Level.WARNING, "writeMulti");
//		session.getBasicRemote().sendBinary(ByteBuffer.wrap(buf));
//	}
//}