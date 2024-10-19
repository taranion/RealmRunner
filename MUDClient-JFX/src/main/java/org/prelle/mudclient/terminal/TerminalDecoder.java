package org.prelle.mudclient.terminal;

import java.util.Iterator;

/**
 * 
 */
public class TerminalDecoder {
	
	private static enum State {
		TEXT,
		ESC_STARTED,
		ESC_COMMAND
	}
	
	private String raw;
	private Iterator<Integer> it;
	private State state;
	
	private StringBuffer buf = new StringBuffer();
	
	//-------------------------------------------------------------------
	public TerminalDecoder(String raw) {
		this.raw = raw;
		reset();
	}
	
	//-------------------------------------------------------------------
	public void reset() {
		it = raw.chars().iterator();
		state = State.TEXT;
		buf = new StringBuffer();
	}

	//-------------------------------------------------------------------
	/**
	 * @return
	 */
	public TerminalEvent read() {
		switch (state) {
		case TEXT:
			return readInTEXT();
		}
		return null;
	}
	
	private TextEvent readInTEXT() {
		int c = it.next();
		switch (c) {
		case 27:
			// ESC started
			state = State.ESC_STARTED;
			String txt = buf.toString();
			buf = new StringBuffer();
			return new TextEvent(txt);
		default:
			if (c>=32 && c<127)
				buf.append(c);
			else
				System.err.println("TerminalDecoder: "+c);
		}
		return null;
	}
}
