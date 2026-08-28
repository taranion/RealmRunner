package org.prelle.terminal;

import java.io.ByteArrayOutputStream;
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
import org.prelle.mudevents.PipeEvent;
import org.prelle.mudevents.MUDEventProcessor;
import org.prelle.mudevents.ansi.ANSIEvent;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
public class InputBuffer implements MUDEventProcessor {
	
	private final static Logger logger = System.getLogger("terminal");
	private final static int MAX_HISTORY = 100;

	@Setter @Getter
	private TerminalMode mode = TerminalMode.LINE_MODE;

	private Consumer<byte[]> echoListener;

	private StringBuilder text = new StringBuilder();
	private List<String> history;
	
	//-------------------------------------------------------------------
	/**
	 */
	public InputBuffer() {
		history = new ArrayList<>();
	}

	//-------------------------------------------------------------------
	public void setEchoListener(Consumer<byte[]> echoListener) {
		this.echoListener = echoListener;
	}

	//-------------------------------------------------------------------
	private void enterPressed() {
		history.add(0,text.toString());
		// Ensure history buffer doesn't exceed MAX_HISTORY
		if (history.size() > MAX_HISTORY) {
			history.removeLast(); // Remove the oldest entry
		}
		
		// Eventually an input handler has been set
//		String toSend = (inputHandler != null) ? inputHandler.onInputBufferLine(text.toString()) : text.toString();
//		// Clear the buffer
//		text.setLength(0);
//		if (toSend != null) {
//			logger.log(Level.INFO, "InputBuffer: sending to server: {0}", toSend);
//			try {
////				sink.write(new PrintableFragment(toSend).getRaw());
////				sink.write(C0Code.CR.code());
////				sink.flush();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
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

	@Override
	public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
		logger.log(Level.INFO, "InputBuffer received event: {0}", event);
		// In raw mode, just pass on every event
		if (mode==TerminalMode.RAW) {
			return List.of(event);
		}
		
		if (event instanceof ANSIEvent ansi) {
			return switch (ansi.getFragment()) {
			case PrintableFragment pf -> {
				text.append(pf.getText());
				logger.log(Level.INFO, "InputBuffer now: {0}", text);
				if (echoListener != null) {
					echoListener.accept(pf.getRaw());
				}
				yield List.of();
			}
			case C0Fragment c0 -> {
				switch (c0.getCode()) {
					case CR -> enterPressed();
					case BS -> {
						deleteLast();
						echoDelete();
					}
					default -> logger.log(Level.INFO, "InputBuffer received unhandled C0 code: {0}", c0.getCode());
				}
				yield List.of(event);
			}
			default -> { 
				logger.log(Level.INFO, "InputBuffer received unhandled ANSI fragment: {0}", ansi.getFragment());
				yield List.of(event);
				}
			};
		
		}
		return List.of(event);
	}

	@Override
	public List<PipeEvent> onSendToRemote(PipeEvent event) {
		// TODO Auto-generated method stub
		return List.of(event);
	}
}
