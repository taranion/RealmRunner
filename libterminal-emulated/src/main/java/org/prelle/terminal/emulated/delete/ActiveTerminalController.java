package org.prelle.terminal.emulated.delete;

import java.io.IOException;
import java.lang.System.Logger.Level;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.terminal.emulated.TerminalController;
import org.prelle.terminal.emulated.TerminalModel;

/**
 * Controller with its own thread that reads from the stream
 */
public class ActiveTerminalController extends TerminalController {

	private ANSIInputStream pipeInput;
	private Thread readThread;
	private boolean continueReading;

	//-------------------------------------------------------------------
	public ActiveTerminalController(TerminalModel model, Emulation emul, ANSIInputStream input) {
		super(model,emul);
		pipeInput = input;
		startNewThread();
	}

	//-------------------------------------------------------------------
	private void stopOldThread() {
		readThread.interrupt();
		try {
			readThread.join(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	private void startNewThread() {
		continueReading = true;
		Runnable run = new Runnable() {
			public void run() {
				while (continueReading) {
					try {
						AParsedElement parsed = pipeInput.readFragment();
						handleFragment(parsed);
					} catch (IOException e) {
						logger.log(Level.ERROR, "Error handling read fragment",e);
					}
				}
			}};
		readThread = new Thread(run, "Terminal");
		readThread.start();

	}

	//-------------------------------------------------------------------
	public void setInputStream(ANSIInputStream in) {
		stopOldThread();
		this.pipeInput = in;
		startNewThread();
	}

}
