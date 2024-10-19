package org.prelle.mudclient.network;

/**
 *
 */
public interface LineBufferListener {

	public void lineBufferChanged(String content, int cursorPosition);

	//-------------------------------------------------------------------
	/**
	 * User hit ENTER on the input buffer. Check if any processing should be
	 * done and return the string to send to the MUD.
	 * @param typed User input
	 * @return Command to be send
	 */
	public String processCommandTyped(String typed);

}
