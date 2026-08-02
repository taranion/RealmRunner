package org.prelle.terminal;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.PrintableFragment;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
public class ReadBuffer {

	public static interface ReadBufferHandler {
		/**
		 * @param line User input line, without the trailing CR/LF
		 * @return Text to send to server. If NULL is returned, nothing is sent to the server.
		 */
		String onLineReceived(String line);
	}
	
	private final static Logger logger = System.getLogger("terminal");
	private final static int MAX_HISTORY = 100;

	private DataToTerminalInputStream terminal;
	private Thread readFromServerThread;
	private ANSIOutputStream out;
	@Setter
	private ReadBufferHandler readBufferHandler;
	
	private ANSIInputStream source;

	private StringBuilder text = new StringBuilder();
	private List<String> history;
	
	//-------------------------------------------------------------------
	/**
	 */
	public ReadBuffer(DataToTerminalInputStream terminal) {
		this.terminal = terminal;
		out = new ANSIOutputStream(terminal.getAsOutputStream());
		readFromServerThread = new Thread( () -> run(), "ReadBuffer");
		
		history = new ArrayList<>();
	}

	//-------------------------------------------------------------------
	public void setSource(ANSIInputStream source) {
		this.source = source;
		readFromServerThread.start();
	}

	//-------------------------------------------------------------------
	private void run() {
		while (true) {
			try {
				AParsedElement frag = source.readFragment();
				System.err.println("ReadBuffer: read fragment: " + frag);
				switch (frag) {
				case C0Fragment c0 when c0.getCode()==C0Code.RS -> releaseBuffer();
				default -> {}
				}
				
				synchronized (out) {
					out.write(frag);
					out.flush();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void releaseBuffer() {
		terminal.releaseBuffer();
	}
	

}
