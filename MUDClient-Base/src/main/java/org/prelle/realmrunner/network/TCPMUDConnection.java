package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.prelle.telnet.TelnetInputStream;
import org.prelle.telnet.TelnetOutputStream;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSocketListener;

/**
 * 
 */
public class TCPMUDConnection implements MUDConnection {
	
	private InetAddress host;
	private int port;
	private TelnetSocketListener listener;
	
	private TelnetSocket socket;
	private InputStream in;
	private OutputStream out;

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 */
	public TCPMUDConnection(InetAddress host, int port, TelnetSocketListener listener) throws IOException{
		this.host = host;
		this.port = port;
		this.listener = listener;
		
		socket = new TelnetSocket(host.getHostAddress(), port);
		socket.addSocketListener(listener);
		in =  socket.getInputStream();
		out = socket.getOutputStream();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDConnection#getStreamFromMUD()
	 */
	@Override
	public InputStream getStreamFromMUD() {
		return in;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDConnection#getStreamToMUD()
	 */
	@Override
	public OutputStream getStreamToMUD() {
		return out;
	}

}
