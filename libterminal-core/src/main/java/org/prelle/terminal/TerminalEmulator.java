package org.prelle.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.prelle.ansi.FilteringANSIStream;

/**
 *
 */
public interface TerminalEmulator {

	public TerminalMode getMode();
	public TerminalEmulator setMode(TerminalMode mode);

	public boolean isLocalEchoActive();
	public TerminalEmulator setLocalEchoActive(boolean value);

//	//-------------------------------------------------------------------
//	/**
//	 * Obtain the stream required to write to the terminal
//	 * @return
//	 */
//	public ANSIOutputStream getOutputStream();
//
//	//-------------------------------------------------------------------
//	public ANSIInputStream getInputStream();

	//-------------------------------------------------------------------
	public int[] getConsoleSize() throws IOException;
	public void addConsoleSizeListener(Consumer<int[]> listener);

	//-------------------------------------------------------------------
	/**
	 * @return Array with Input encoding and Output encoding
	 */
	public Charset[] getEncodings();

	//-------------------------------------------------------------------
	public void sendUserInput(String text) ;

	//-------------------------------------------------------------------
	FilteringANSIStream connectWith(InputStream in, OutputStream out);
	
}
