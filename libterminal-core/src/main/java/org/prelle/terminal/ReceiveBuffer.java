package org.prelle.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.CursorUp;
import org.prelle.ansi.commands.EraseInLine;

import lombok.Getter;

/**
 * This class deals with data received from the MUD server. Its run() method is executed 
 * in a dedicated thread and reads from the stream. Every received line is processed by a list
 * of handlers (e.g. for triggers, text-to-speech, etc.). If a handler returns NULL, the 
 * line is considered "swallowed" and is not sent to the terminal.
 */
public class ReceiveBuffer {
	
	@Getter
	public static class ReceivedLine {
		private Instant timestamp = Instant.now();
		private List<AParsedElement> originalAnsi = new ArrayList<>();
		private String originalAsText;
		private byte[] raw;

		public static byte[] getFinalRaw(List<AParsedElement> finalAnsi) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (AParsedElement elem : finalAnsi) {
				byte[] b = elem.getRaw();
				if (b != null) {
					baos.writeBytes(b);
				}
			}
			return baos.toByteArray();
		}
	}
	
	public static record HandlerResult(boolean stopProcessing, boolean deletePrevious, List<AParsedElement> replaceWith) { }
	
	public final static HandlerResult NO_CHANGE = new HandlerResult(false, false, null);

	public static interface ReadBufferHandler {
		/**
		 * Update the `line.sentToTerminal` with the text to send to the terminal.
		 * @param line User input line, without the trailing CR/LF
		 * @return Text to send to server. If TRUE is returned, the line is considered "consumed" and processing of this line should stop and the line not being sent to the server.
		 */
		HandlerResult onLineReceived(ReceivedLine line, List<ReceivedLine> history);
		void onConnectionLost();
	}
	
	private final static Logger logger = System.getLogger("terminal");
	private final static int MAX_HISTORY = 100;

	private DataToTerminalInputStream terminal;
	private Thread readFromServerThread;
	private ANSIOutputStream out;
	private List<ReadBufferHandler> readBufferHandler = new ArrayList<>();
	
	private ANSIInputStream source;

	private StringBuilder collectText = new StringBuilder();
	private List<AParsedElement> collectANSI = new ArrayList<>();
	private ReceivedLine currentLine;
	private List<ReceivedLine> history;
	
	//-------------------------------------------------------------------
	/**
	 */
	public ReceiveBuffer(DataToTerminalInputStream terminal) {
		this.terminal = terminal;
		out = new ANSIOutputStream(terminal.getAsOutputStream());
		readFromServerThread = new Thread( () -> run(), "ReadBuffer");
		
		history = new ArrayList<>();
	}
	
	//-------------------------------------------------------------------
	public void addReadBufferHandler(ReadBufferHandler handler) {
		if (!readBufferHandler.contains(handler))
			readBufferHandler.add(handler);
	}

	//-------------------------------------------------------------------
	public void setSource(ANSIInputStream source) {
		this.source = source;
		readFromServerThread.start();
	}

	//-------------------------------------------------------------------
	private void run() {
		try {
			currentLine = new ReceivedLine();
			while (true) {
				try {
					AParsedElement frag = source.readFragment();
					logger.log(Level.TRACE, "read fragment: " + frag+" ");
					if (frag==null) {
						logger.log(Level.WARNING, "Connection lost");
						for (ReadBufferHandler handler : readBufferHandler) {
							handler.onConnectionLost();
						}
						return;
					}
					ReceivedLine toSend = 
					switch (frag) {
					case C0Fragment c0 when c0.getCode()==C0Code.RS -> releaseBuffer(c0);
					case C0Fragment c0 when c0.getCode()==C0Code.CR -> releaseBuffer(c0);
					case C0Fragment c0 when c0.getCode()==C0Code.LF -> releaseBuffer(c0);
					case PrintableFragment print -> {
						collectANSI.add(print);
						collectText.append(print.getText());
						yield null;
					}
					default -> {
						collectANSI.add(frag);
						yield null;
						}
					}
					;
					
					synchronized (out) {
						out.write(frag);
						out.flush();
					}
				} catch (IOException e) {
					logger.log(Level.WARNING, "IOException reading from server", e);
					for (ReadBufferHandler handler : readBufferHandler) {
						handler.onConnectionLost();
					}
					return;
				}
			}
		} finally {
			logger.log(Level.INFO, "ReadBuffer thread exiting");
		}
	}

	//-------------------------------------------------------------------
	public ReceivedLine releaseBuffer(C0Fragment code) {
		currentLine.originalAnsi = collectANSI;
		currentLine.originalAsText = collectText.toString();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (AParsedElement elem : collectANSI) {
			byte[] b = elem.getRaw();
			if (b != null) {
				baos.writeBytes(b);
			}
		}
		currentLine.raw = baos.toByteArray();

		String line = collectText.toString();
		logger.log(Level.DEBUG, "RCV: "+line);
		
//		logger.log(Level.DEBUG, "Find {0} listeners", readBufferHandler.size());
		
		boolean deletePrevious = false;
		List<AParsedElement> reallySend = null; //currentLine.originalAnsi;
		for (ReadBufferHandler handler : readBufferHandler) {
//			logger.log(Level.DEBUG, "Consult handler {0}", handler.getClass());
			HandlerResult result = handler.onLineReceived(currentLine, history.subList(0, Math.min(5, history.size())));
			if (!deletePrevious) deletePrevious = result.deletePrevious();
			if (result.replaceWith()!=null) reallySend = result.replaceWith();
			if (result.stopProcessing()) {
				break;
			}
		}
		// Is there something left to send to the terminal? 
		if (deletePrevious) {
			// Move the cursor up one line and clear it
			terminal.writeToTerminal( (new EraseInLine(EraseInLine.Mode.LINE)).getRaw() );
			terminal.writeToTerminal("\r".getBytes());
		}
		
		if (reallySend!=null) {
			terminal.writeToTerminal(currentLine.getFinalRaw(reallySend));
		}

		history.add(currentLine);
		// Make sure the history doesn't grow too large
		if (history.size()>MAX_HISTORY) {
			history.removeFirst();
		}

		terminal.releaseBuffer();
		ReceivedLine toReturn = currentLine;
		// Reset the buffers for the next line
		currentLine = new ReceivedLine();
		collectText.setLength(0);
		collectANSI = new ArrayList<>();
		
		return toReturn;
	}
	

}
