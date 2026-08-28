package org.prelle.terminal.emulated;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;

import org.prelle.ansi.FilteringANSIStream;
import org.prelle.mudevents.MUDEventPipeline;
import org.prelle.mudevents.PipeEvent;
import org.prelle.terminal.MessageLog;
import org.prelle.terminal.ReceiveBuffer;
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
	public FilteringANSIStream connectWith(MessageLog log, InputStream in, OutputStream out) {
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
	public ReceiveBuffer getReadBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.PipeEvent)
	 */
	public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
		System.getLogger("TERM").log(Logger.Level.INFO, "TerminalEmulatorModel received event: {0}", event);
		return List.of(event);
	}

	@Override
	public List<PipeEvent> onSendToRemote(PipeEvent event) {
		// TODO Auto-generated method stub
		return List.of(event);
	}

	@Override
	public void connectWith(MUDEventPipeline pipeOut) {
		// TODO Auto-generated method stub
		
	}

}
