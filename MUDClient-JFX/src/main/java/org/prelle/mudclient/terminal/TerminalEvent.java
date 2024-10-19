package org.prelle.mudclient.terminal;

/**
 * 
 */
public interface TerminalEvent {

	public static enum Type {
		TEXT,
		GRAPHIC_RESET
	}
	
	public Type getType();
	
}
