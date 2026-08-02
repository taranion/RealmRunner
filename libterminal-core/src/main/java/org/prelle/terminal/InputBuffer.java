package org.prelle.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.CursorBackward;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
public class InputBuffer {

	public static interface InputBufferHandler {
		/**
		 * @param line User input line, without the trailing CR/LF
		 * @return Text to send to server. If NULL is returned, nothing is sent to the server.
		 */
		String onInputBufferLine(String line);
	}
	
	private final static Logger logger = System.getLogger("terminal");
	private final static int MAX_HISTORY = 100;

	@Setter @Getter
	private TerminalMode mode = TerminalMode.LINE_MODE;

	private Thread readFromTerminalThread;
	private ANSIInputStream in;
	private Consumer<byte[]> echoListener;
	@Setter
	private InputBufferHandler inputHandler;
	
	private ANSIOutputStream sink;

	private StringBuilder text = new StringBuilder();
	private List<String> history;
	
	//-------------------------------------------------------------------
	/**
	 */
	public InputBuffer(DataFromTerminalOutputStream terminal) {
		in = new ANSIInputStream(terminal.getAsInputStream());
		in.setCollectPrintable(false);
		readFromTerminalThread = new Thread( () -> run(), "InputBuffer");
		
		history = new ArrayList<>();
	}

	//-------------------------------------------------------------------
	public void setSink(ANSIOutputStream sink) {
		this.sink = sink;
		readFromTerminalThread.start();
	}

	//-------------------------------------------------------------------
	public void setEchoListener(Consumer<byte[]> echoListener) {
		this.echoListener = echoListener;
	}

	//-------------------------------------------------------------------
	public void stop() {
		readFromTerminalThread.interrupt();
	}
	
	//-------------------------------------------------------------------
	private void run() {
		while (true) {
			try {
				AParsedElement frag = in.readFragment();
				logger.log(Level.INFO,"InputBuffer: read fragment: " + frag+" - mode is " + mode);
				if (mode==TerminalMode.RAW) {
					// In RAW mode don't filter anything, just pass it to the sink
					sink.write(frag);
					sink.flush();
				} else {
					processInLineMode(frag);
				}
				
				// If an EchoListener has been defined, echo all Printable and C0 fragments
				if (echoListener != null) {
					switch (frag) {
					case PrintableFragment _ -> echoListener.accept(frag.getRaw());
					case C0Fragment c0 when c0.getCode()==C0Code.DEL -> echoDelete();
					case C0Fragment c0 ->  {
						echoListener.accept(frag.getRaw());
						if (c0.getCode()==C0Code.CR) {
							echoListener.accept("\r\n".getBytes());
						}
					}
					default -> {
						
						sink.write(frag);
					}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void processInLineMode(AParsedElement frag) {
		switch (frag) {
		case PrintableFragment pf -> {
				text.append(pf.getText());
				logger.log(Level.INFO, "InputBuffer now: {0}", text);
		}
		case C0Fragment c0 -> {
			switch (c0.getCode()) {
			case CR ->  enterPressed();
			case DEL -> deleteLast();
			default ->  {
				logger.log(Level.WARNING, "TODO: C0Fragment: {0} - text is {1}", c0, text);
			}
			}
		} // case C0
		default -> {
			// Ignore other fragment types
		}
		} // switch
	}

	//-------------------------------------------------------------------
	private void enterPressed() {
		history.add(0,text.toString());
		// Ensure history buffer doesn't exceed MAX_HISTORY
		if (history.size() > MAX_HISTORY) {
			history.removeLast(); // Remove the oldest entry
		}
		
		// Eventually an input handler has been set
		String toSend = (inputHandler != null) ? inputHandler.onInputBufferLine(text.toString()) : text.toString();
		// Clear the buffer
		text.setLength(0);
		if (toSend != null) {
			logger.log(Level.INFO, "InputBuffer: sending to server: {0}", toSend);
			try {
				sink.write(new PrintableFragment(toSend).getRaw());
				sink.write(C0Code.CR.code());
				sink.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//-------------------------------------------------------------------
	private void deleteLast() {
		if (text.length() > 0) {
			text.deleteCharAt(text.length() - 1);
			logger.log(Level.INFO, "DEL: InputBuffer now: {0}", text);
//			try {
//				sink.write(new CursorBackward(2).getRaw());
//				sink.write((int)' '); // Overwrite with space
//				sink.write(new CursorBackward(1).getRaw());
//				sink.flush();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}
	
	//-------------------------------------------------------------------
	private void echoDelete() {
		logger.log(Level.INFO, "echoDelete");
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(C0Code.BS.code());
			baos.write((int)' '); // Overwrite with space
			baos.write(C0Code.BS.code());
			if (echoListener != null) {
				echoListener.accept(baos.toByteArray());
			}
			baos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
