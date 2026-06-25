package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.lang.System.Logger.Level;
import java.util.Arrays;

import com.techsenger.jeditermfx.core.TtyConnector;

/**
 *
 */
public class JediTtyConnector implements TtyConnector {

	private final static System.Logger logger = System.getLogger("jedi.terminal");

	public static class ConnectorOutputStream extends OutputStream {
		private JediTtyConnector connector;

		public ConnectorOutputStream(JediTtyConnector value) {
			this.connector=value;
		}

		@Override public void write(int b) throws IOException {
			connector.write(new byte[] {(byte)b});
		}
		@Override public void write(byte[] b) throws IOException {
	      connector.write(b);
	    }
	}

	public static class ConnectorInputStream extends InputStream {
		private JediTtyConnector connector;

		public ConnectorInputStream(JediTtyConnector value) {
			this.connector=value;
		}

		@Override
		public int read() throws IOException {
			char[] buf = new char[1];
			return connector.read(buf, 0, 1);
		}
		public int readNBytes(byte[] b, int off, int len) throws IOException {
			char[] buf = new char[b.length];
			return connector.read(buf, off, len);
		}
	}

	private PipedInputStream pipeIn;

	//-------------------------------------------------------------------
	/**
	 */
	public JediTtyConnector() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int read(char[] buf, int offset, int length) throws IOException {
		logger.log(Level.INFO, "read");
		return 0;
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		logger.log(Level.INFO, "write "+Arrays.toString(bytes));

	}

	// Write to terminal
	@Override
	public void write(String value) throws IOException {
		logger.log(Level.INFO, "write "+value);

	}

	@Override
	public boolean isConnected() {
		logger.log(Level.INFO, "isConnected ");
		return true;
	}

	@Override
	public int waitFor() throws InterruptedException {
		logger.log(Level.INFO, "waitFor ");
		return 0;
	}

	@Override
	public boolean ready() throws IOException {
		logger.log(Level.INFO, "ready ");
		return true;
	}

	@Override
	public String getName() {
		logger.log(Level.INFO, "getName ");
		return null;
	}

	@Override
	public void close() {
		logger.log(Level.INFO, "close ");

	}

}
