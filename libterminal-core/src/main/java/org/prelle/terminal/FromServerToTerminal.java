package org.prelle.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.io.PipedWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;

/**
 * 
 */
public class FromServerToTerminal implements Runnable {
	
	private final static Logger logger = System.getLogger("terminal");
	
	private InputStream source;
	private PipedWriter writeToTerminalWriter;
	private PipedOutputStream writeToTerminalStream;
	private Charset encoding;

	//-------------------------------------------------------------------
	public FromServerToTerminal(InputStream source, PipedWriter writeToTerminal, Charset encoding) {
		this.source = source;
		this.writeToTerminalWriter = writeToTerminal;
		this.encoding = encoding;
	}

	//-------------------------------------------------------------------
	public FromServerToTerminal(InputStream source, PipedOutputStream writeToTerminal, Charset encoding) {
		this.source = source;
		this.writeToTerminalStream = writeToTerminal;
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
				if (logger.isLoggable(Level.TRACE)) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < len; i++) {
						sb.append(String.format("%02X ", buffer[i]));
					}
					logger.log(Level.TRACE, "Read data (hex): {0}", sb.toString());
				}
				if (writeToTerminalWriter!=null) {
					// Convert read data into a string using the specified encoding, then write it to the terminal
					String str = new String(buffer, 0, len, encoding);
					writeToTerminalWriter.write(str);
				} else {
					writeToTerminalStream.write(buffer, 0, len);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		logger.log(Level.WARNING, "LEAVE: run()");
	}

}
