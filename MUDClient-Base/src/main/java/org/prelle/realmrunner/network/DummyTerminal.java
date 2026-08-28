package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;

import org.prelle.ansi.FilteringANSIStream;
import org.prelle.mudevents.PipeEvent;
import org.prelle.mudevents.MUDEventPipeline;
import org.prelle.terminal.MessageLog;
import org.prelle.terminal.ReceiveBuffer;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

/**
 * 
 */
public class DummyTerminal implements TerminalEmulator {

	//-------------------------------------------------------------------
	/**
	 */
	public DummyTerminal() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getMode()
	 */
	@Override
	public TerminalMode getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setMode(org.prelle.terminal.TerminalMode)
	 */
	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#isLocalEchoActive()
	 */
	@Override
	public boolean isLocalEchoActive() {
		// TODO Auto-generated method stub
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setLocalEchoActive(boolean)
	 */
	@Override
	public TerminalEmulator setLocalEchoActive(boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getReadBuffer()
	 */
	@Override
	public ReceiveBuffer getReadBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getConsoleSize()
	 */
	@Override
	public int[] getConsoleSize() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#addConsoleSizeListener(java.util.function.Consumer)
	 */
	@Override
	public void addConsoleSizeListener(Consumer<int[]> listener) {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getEncodings()
	 */
	@Override
	public Charset[] getEncodings() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#releaseInputBuffer()
	 */
	@Override
	public void releaseInputBuffer() {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#sendUserInput(java.lang.String)
	 */
	@Override
	public void sendUserInput(String text) {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#connectWith(org.prelle.terminal.MessageLog, java.io.InputStream, java.io.OutputStream)
	 */
	@Override
	public FilteringANSIStream connectWith(MessageLog log, InputStream in, OutputStream out) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#start()
	 */
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
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.PipeEvent)
	 */
	@Override
	public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
		System.err.println("DummyTerminal received event: " + event);
		return List.of();
	}

	@Override
	public void connectWith(MUDEventPipeline pipeOut) {
		// TODO Auto-generated method stub
		
	}

}
