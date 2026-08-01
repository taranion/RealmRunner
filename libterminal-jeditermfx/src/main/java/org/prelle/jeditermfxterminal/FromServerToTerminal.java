package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;

/**
 * 
 */
class FromServerToTerminal implements Runnable {
	
	private final static Logger logger = JediTerminalView.logger;
	
	private InputStream source;
	private PipedWriter writeToTerminal;
	private Charset encoding;

	//-------------------------------------------------------------------
	/**
	 * @param widget 
	 */
	public FromServerToTerminal(InputStream source, PipedWriter writeToTerminal, Charset encoding) {
		this.source = source;
		this.writeToTerminal = writeToTerminal;
		this.encoding = encoding;
	}

	//-------------------------------------------------------------------
	public void setSource(InputStream source) {
		this.source = source;
	}

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
				int len = source.read(buffer);
				if (len < 0) {
					logger.log(Level.INFO, "End of stream reached");
					break;
				}
//				logger.log(Level.INFO, "Read {0} bytes from source", len);
				// Dump as ASCII code hex values
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < len; i++) {
					sb.append(String.format("%02X ", buffer[i]));
				}
//				logger.log(Level.INFO, "Read data (hex): {0}", sb.toString());
				// Convert read data into a string using the specified encoding, then write it to the terminal
				String str = new String(buffer, 0, len, encoding);
				writeToTerminal.write(str);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		logger.log(Level.WARNING, "LEAVE: run()");
	}

}
