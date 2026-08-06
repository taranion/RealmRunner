package org.prelle.terminal;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 
 */
public  class MessageLog {
	
	public static enum Layer {
		TELNET,
		ANSI,
		GMCP
	}

	private PrintWriter out;
	
	//-------------------------------------------------------------------
	public MessageLog() {
		try {
			out = new PrintWriter("/tmp/realmrunner_messages.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public synchronized void log(boolean send, Layer layer, String message) {
		StringBuilder sb = new StringBuilder();
		sb.append(send ? "SEND" : "RECV");
		sb.append(" [").append(layer).append("] ");
		sb.append(message);
		if (out!=null)
		out.println(sb.toString());
		out.flush();
	}

}
