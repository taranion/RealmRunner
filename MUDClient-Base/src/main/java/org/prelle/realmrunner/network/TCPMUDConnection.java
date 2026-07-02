package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import lombok.Getter;

/**
 * 
 */
public class TCPMUDConnection implements MUDConnection {
	
	@Getter private InetAddress host;
	@Getter private int port;
	
	private Socket socket;
	private InputStream in;
	private OutputStream out;

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 */
	public TCPMUDConnection(InetAddress host, int port) throws IOException{
		this.host = host;
		this.port = port;
		
		socket = new Socket(host.getHostAddress(), port);
		in =  socket.getInputStream();
		out = socket.getOutputStream();
		socket.setSoTimeout(100);
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
