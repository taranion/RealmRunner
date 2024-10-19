package org.prelle.terminal.emulated;

import java.io.IOException;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
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
	public ANSIOutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ANSIInputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getConsoleSize() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

}
