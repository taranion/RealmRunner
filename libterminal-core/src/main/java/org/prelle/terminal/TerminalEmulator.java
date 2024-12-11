package org.prelle.terminal;

import java.io.IOException;
import java.nio.charset.Charset;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;

/**
 *
 */
public interface TerminalEmulator {

	public TerminalMode getMode();
	public TerminalEmulator setMode(TerminalMode mode);

	public boolean isLocalEchoActive();
	public TerminalEmulator setLocalEchoActive(boolean value);

	//-------------------------------------------------------------------
	/**
	 * Obtain the stream required to write to the terminal
	 * @return
	 */
	public ANSIOutputStream getOutputStream();

	//-------------------------------------------------------------------
	public ANSIInputStream getInputStream();

	//-------------------------------------------------------------------
	public int[] getConsoleSize() throws IOException, InterruptedException;

	//-------------------------------------------------------------------
	/**
	 * @return Array with Input encoding and Output encoding
	 */
	public Charset[] getEncodings();
}
