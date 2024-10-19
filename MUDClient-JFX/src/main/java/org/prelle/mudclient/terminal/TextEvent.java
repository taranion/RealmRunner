package org.prelle.mudclient.terminal;

/**
 * 
 */
public class TextEvent implements TerminalEvent {
	
	private String text;

	//-------------------------------------------------------------------
	public TextEvent(String data) {
		this.text = data;
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudclient.terminal.TerminalEvent#getType()
	 */
	@Override
	public Type getType() {
		return Type.TEXT;
	}

	//-------------------------------------------------------------------
	public String getText() {
		return text;
	}

}
