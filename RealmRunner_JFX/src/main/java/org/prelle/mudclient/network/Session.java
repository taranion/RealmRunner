package org.prelle.mudclient.network;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSocketListener;
import org.prelle.telnet.TelnetSubnegotiationHandler;
import org.prelle.telnet.TelnetSocket.State;
import org.prelle.telnet.mud.MUDTilemapProtocol.TileMapData;

/**
 *
 */
public class Session implements Runnable, TelnetSocketListener {

	private final static Logger logger = System.getLogger("mud.client.session");

	private String server;
	private int port;
	private transient TelnetSocket socket;
	private transient Thread thread;
	private transient Thread bufferThread;
	private transient SessionListener listener;
	private transient int lastReceiveBufSize;

	private List<Integer> receiveBuf = new ArrayList<>();

	//-------------------------------------------------------------------
	public Session(String server, int port) {
		this.server = server;
		this.port   = port;
	}

	//-------------------------------------------------------------------
	/**
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connect(SessionListener listener) throws IOException {
		this.listener = listener;
		socket = new TelnetSocket(server, port);
		socket.addSocketListener(this);
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		bufferThread = new Thread(() -> runBufferCheck(), "BufferCheck");
		bufferThread.start();
		try {
			InputStream in = socket.getInputStream();
			while (true) {
				if (in.available()==0) {
					synchronized (receiveBuf) {
						receiveBuf.notify();
					}
				}

				int foo = in.read();
				if (foo==-1) {
					logger.log(Level.WARNING, "Connection lost");
					if (listener!=null)
						listener.connectionLost(this);
					break;
				}
//				synchronized (receiveBuf) {
					receiveBuf.add(foo);
//				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			logger.log(Level.WARNING, "Stop receiving");
		}
	}

	private void runBufferCheck() {
		while (true) {
			synchronized (receiveBuf) {
				try {
					receiveBuf.wait(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				if (receiveBuf.size()!=lastReceiveBufSize) {
//					lastReceiveBufSize = receiveBuf.size();
//				} else {
//					// No change
					if (!receiveBuf.isEmpty()) {
						byte[] buf = new byte[receiveBuf.size()];
						for (int i=0; i<buf.length; i++) {
							buf[i] = (byte)(int)receiveBuf.get(i);
						}
						receiveBuf.clear();
						String line = new String(buf, Charset.defaultCharset());
						if (listener!=null) {
							listener.textReceived(line);
						} else {
							System.out.print(line);
							System.out.flush();
						}
					}

//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
			}
		}
	}

	//-------------------------------------------------------------------
	public void sendMessage(String text) {
		logger.log(Level.DEBUG, "Send {0}",text);
		byte[] data = (text+"\r\n").getBytes(Charset.defaultCharset());
		try {
			socket.getOutputStream().write(data);
			socket.getOutputStream().flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.telnet.TelnetSocketListener#telnetOptionDataChanged(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetSubnegotiationHandler, java.lang.Object)
//	 */
//	@Override
//	public void telnetOptionDataChanged(TelnetSocket nvt, TelnetSubnegotiationHandler option, Object data) {
//		// TODO Auto-generated method stub
//		logger.log(Level.INFO, "RCV Telnet Option "+data);
//
//		if (data instanceof TileMapData) {
//			// TODO: Put into receiveBuf instead of directly calling listener
//			if (listener!=null)
//				listener.mapReceived((TileMapData) data);
//		}
//	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetOptionStatusChange(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetSubnegotiationHandler, boolean)
	 */
	@Override
	public void telnetOptionStatusChange(TelnetSocket nvt, TelnetOption option, boolean active) {
		logger.log(Level.INFO, "Feature {0} is {1}", option.name(), active?"enabled":"disabled");
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetSocketChanged(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetSocket.State, org.prelle.telnet.TelnetSocket.State)
	 */
	@Override
	public void telnetSocketChanged(TelnetSocket nvt, State oldState, State newState) {
		logger.log(Level.DEBUG, "state changed "+newState);
	}

}
