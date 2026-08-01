package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * 
 */
class FromTerminalToServer implements Runnable {
	
	private final static Logger logger = JediTerminalView.logger;
	
	private PipedInputStream readFromTerminal;
	private OutputStream writeToServer;

	//-------------------------------------------------------------------
	/**
	 * @param widget 
	 */
	public FromTerminalToServer(PipedInputStream source, OutputStream writeToTerminal) {
		this.readFromTerminal = source;
		this.writeToServer = writeToTerminal;
	}

//	//-------------------------------------------------------------------
//	public void setSink(OutputStream source) {
//		this.source = source;
//	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.log(Level.WARNING, "ENTER: run()");
		byte[] buffer = new byte[1024];
		while (true) {
			try {
				int len = readFromTerminal.read(buffer);
				if (len < 0) {
					logger.log(Level.INFO, "End of stream reached");
					break;
				}
				writeToServer.write(buffer, 0, len);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		logger.log(Level.WARNING, "LEAVE: run()");
	}

}
