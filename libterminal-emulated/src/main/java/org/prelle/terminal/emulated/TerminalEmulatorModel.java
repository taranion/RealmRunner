package org.prelle.terminal.emulated;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.prelle.ansi.FilteringANSIStream;
import org.prelle.terminal.ReadBuffer;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

/**
 *
 */
public class TerminalEmulatorModel implements TerminalEmulator {



	//-------------------------------------------------------------------
	/**
	 */
	public TerminalEmulatorModel() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public TerminalMode getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLocalEchoActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TerminalEmulator setLocalEchoActive(boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getConsoleSize() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Charset[] getEncodings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addConsoleSizeListener(Consumer<int[]> listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FilteringANSIStream connectWith(InputStream in, OutputStream out) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendUserInput(String text) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void releaseInputBuffer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#close()
	 */
	@Override
	public void close() {
	}

	@Override
	public ReadBuffer getReadBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

}
