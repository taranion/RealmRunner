package org.prelle.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedWriter;
import java.io.Writer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * 
 */
public class FromTerminalToServer implements Runnable {
	
	private final static Logger logger = System.getLogger("terminal");
	
	private PipedInputStream readFromTerminal;
	private OutputStream writeToServer;
	
	private boolean localEcho = true;
	private OutputStream echoStream;
	private Writer echoWriter;

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
				logger.log(Level.INFO, "Read from terminal: {0} bytes \t= {1}", len, new String(buffer, 0, len));
				writeToServer.write(buffer, 0, len);
				writeToServer.flush();
				
				if (localEcho) {
					if (echoWriter != null) {
						echoWriter.write(new String(buffer, 0, len));
						echoWriter.flush();
					} else if (echoStream != null) {
						echoStream.write(buffer, 0, len);
						echoStream.flush();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		logger.log(Level.WARNING, "LEAVE: run()");
	}

	//-------------------------------------------------------------------
	/**
	 * @param echoStream the echoStream to set
	 */
	public void setEchoStream(OutputStream echoStream) {
		this.echoStream = echoStream;
	}

	//-------------------------------------------------------------------
	public void setEchoStream(PipedWriter writeToTerminal) {
		this.echoWriter = writeToTerminal;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the localEcho
	 */
	public boolean isLocalEcho() {
		return localEcho;
	}

	//-------------------------------------------------------------------
	/**
	 * @param localEcho the localEcho to set
	 */
	public void setLocalEcho(boolean localEcho) {
		this.localEcho = localEcho;
	}

}
