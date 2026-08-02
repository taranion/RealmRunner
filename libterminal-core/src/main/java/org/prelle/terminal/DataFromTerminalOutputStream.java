package org.prelle.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;

/**
 * This class helps with the need from the terminal to write into an OutputStream,
 * while the same time the application needs to read an process the keystrokes from the
 * terminal (e.g. for alias processing). 
 * This class provides a ring buffer that can be written to and read from.
 */
public class DataFromTerminalOutputStream extends OutputStream {
	
	private final static Logger logger = System.getLogger("terminal");
	
	private static class ReadFromRingBufferInputStream extends InputStream {
		private final DataFromTerminalOutputStream ringBuffer;
		
		public ReadFromRingBufferInputStream(DataFromTerminalOutputStream ringBuffer) {
			this.ringBuffer = ringBuffer;
		}
		
		@Override
		public int read() throws IOException {
			return ringBuffer.read();
		}
		
		@Override
		public int available() throws IOException {
			synchronized (ringBuffer.ringBuffer) {
				return ringBuffer.available;
			}
		}		
	}
	
	private final static int BUFFER_SIZE = 1024;
	
	private byte[] ringBuffer = new byte[BUFFER_SIZE];
	private int readPos = 0;
	private int available = 0;

	private int writePos = 0;
	private int capacity = BUFFER_SIZE;
	
	private ReadFromRingBufferInputStream inputStream;

	//-------------------------------------------------------------------
	/**
	 */
	public DataFromTerminalOutputStream() {
		inputStream = new ReadFromRingBufferInputStream(this);
	}

	//-------------------------------------------------------------------
	public InputStream getAsInputStream() {
		return inputStream;
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
//		logger.log(Logger.Level.INFO, "ENTER: write: b={0}", b);
		synchronized (ringBuffer) {
			// Wait until there is space in the buffer
			waitForCapacity();
			if (capacity==0) {
				// Failed to write
				throw new IOException("Buffer overflow: SIZE="+BUFFER_SIZE);
			}
			
			ringBuffer[ writePos ] = (byte) b;
			writePos = (writePos + 1) % BUFFER_SIZE;  // Eventually wrap around
			available++;
			capacity--;
			
			ringBuffer.notifyAll();
		}
//		logger.log(Logger.Level.INFO, "LEAVE: write: b={0}", b);
	}

	//-------------------------------------------------------------------
	private void waitForCapacity() {
		synchronized (ringBuffer) {
			while (capacity == 0) {
				try {
					ringBuffer.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	//-------------------------------------------------------------------
	private int read() throws IOException {
//		logger.log(Logger.Level.INFO, "ENTER: read");
		synchronized (ringBuffer) {
			while (available == 0) {
				try {
					ringBuffer.wait();
				} catch (InterruptedException e) {
				}
			}
			
			int b = ringBuffer[ readPos ] & 0xFF; // Convert to unsigned
			readPos = (readPos + 1) % BUFFER_SIZE; // Eventually wrap around
			available--;
			capacity++;
			
			ringBuffer.notifyAll();
//			logger.log(Logger.Level.INFO, "LEAVE: read");
			return b;
		}
	}
}
