package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import org.prelle.mudevents.BinaryDataEvent;
import org.prelle.mudevents.MUDEvent;
import org.prelle.mudevents.StartEvent;

import lombok.Getter;

/**
 * 
 */
public class TCPSSLMUDConnection extends MUDConnection {
	
	private final static Logger logger = System.getLogger("mud.client.ssl");
	
	@Getter private InetAddress host;
	@Getter private int port;
	
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private Thread readThread;
	private boolean closed;

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 */
	public TCPSSLMUDConnection(InetAddress host, int port) throws IOException{
		this.host = host;
		this.port = port;
		this.supportsTelnet = true;
		
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        socket = sslsocketfactory.createSocket(host.getHostAddress(), port);
		
		in =  socket.getInputStream();
		out = socket.getOutputStream();
//		socket.setSoTimeout(100);
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#getName()
	 */
	@Override
	public String getName() {
		return "TCPSSL";
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.MUDEvent)
	 */
	@Override
	public List<MUDEvent> apply(MUDEvent event) {
		if (event instanceof BinaryDataEvent binary) {
			try {
				out.write(binary.getData());
			} catch (IOException e) {
				logger.log(Level.WARNING, "IO error while sending data to MUD: {0}", e.getMessage());
				receivePipe.publish(new ConnectionLostEvent(e.toString()));
			}
		}
		return null;
	}

	@Override
	public void start() {
		Runnable read = () -> {
			try {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					byte[] data = new byte[bytesRead];
					System.arraycopy(buffer, 0, data, 0, bytesRead);
					receivePipe.publish(new BinaryDataEvent(this,data));
				}
				closed = true;
			} catch (IOException e) {
				logger.log(Level.WARNING, "IO error while reading from MUD: {0}", e.getMessage());
				receivePipe.publish(new ConnectionLostEvent(e.toString()));
			}
		};
		Thread.startVirtualThread(read);
		receivePipe.publish(new StartEvent(this));
	}

	@Override
	public void close() {
		closed = true;
		readThread.interrupt();		
	}

}
