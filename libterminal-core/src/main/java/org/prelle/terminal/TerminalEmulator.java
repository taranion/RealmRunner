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
	/**
	 * Return the buffer that receives data from the server. You can register trigger or MSP handler here.
	 */
	public ReceiveBuffer getReadBuffer();
	
	//-------------------------------------------------------------------
	public int[] getConsoleSize() throws IOException;
	public void addConsoleSizeListener(Consumer<int[]> listener);

	//-------------------------------------------------------------------
	/**
	 * @return Array with Input encoding and Output encoding
	 */
	public Charset[] getEncodings();

	//-------------------------------------------------------------------
	public void releaseInputBuffer();

	//-------------------------------------------------------------------
	public void sendUserInput(String text) ;

	//-------------------------------------------------------------------
	/** 
	 * Setup streams, but do not start transmitting/receiving yet.
	 */
	FilteringANSIStream connectWith(MessageLog log, InputStream in, OutputStream out);

	//-------------------------------------------------------------------
	void start();

	//-------------------------------------------------------------------
	void close();
	
}
