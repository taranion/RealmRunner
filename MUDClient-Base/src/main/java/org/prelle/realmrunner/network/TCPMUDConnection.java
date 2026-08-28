package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import org.prelle.mudevents.BinaryDataEvent;
import org.prelle.mudevents.PipeEvent;
import org.prelle.mudevents.StartEvent;

import lombok.Getter;

/**
 * 
 */
public class TCPMUDConnection extends MUDConnection {
	
	private final static Logger logger = System.getLogger("mud.client.tcp");
	
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
	public TCPMUDConnection(InetAddress host, int port) throws IOException{
		this.host = host;
		this.port = port;
		this.supportsTelnet = true;
		
		socket = new Socket(host.getHostAddress(), port);
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
		return "TCP";
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.PipeEvent)
	 */
	@Override
	public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
		if (event instanceof BinaryDataEvent binary) {
			try {
				out.write(binary.getData());
			} catch (IOException e) {
				logger.log(Level.WARNING, "IO error while sending data to MUD: {0}", e.getMessage());
				receivePipe.publish(new ConnectionLostEvent());
			}
		} else
			logger.log(Level.WARNING, "Unexpected event type received in TCPMUDConnection: {0}", event.getClass().getName());
		return List.of();
	}

	@Override
	public List<PipeEvent> onSendToRemote(PipeEvent event) {
		// TODO Auto-generated method stub
		return List.of(event);
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
					receivePipe.publish(new BinaryDataEvent(data));
				}
				closed = true;
			} catch (IOException e) {
				logger.log(Level.WARNING, "IO error while reading from MUD: {0}", e.getMessage());
				receivePipe.publish(new ConnectionLostEvent());
			}
		};
		Thread.startVirtualThread(read);
		receivePipe.publish(new StartEvent());
	}

	@Override
	public void close() {
		closed = true;
		readThread.interrupt();		
	}

}
