package org.prelle.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;

/**
 * This class helps with the need from the terminal to read from an InputStream,
 * while the same time the application needs to be able to write directly on the terminal.
 * This class provides a ring buffer that can be written to and read from.
 */
public class DataToTerminalInputStream extends InputStream {
	
	private final static Logger logger = System.getLogger("terminal");
	
	private static class ReadFromRingBufferOutputStream extends OutputStream {
		private final DataToTerminalInputStream ringBuffer;
		
		public ReadFromRingBufferOutputStream(DataToTerminalInputStream ringBuffer) {
			this.ringBuffer = ringBuffer;
		}
		
		@Override
		public void write(int data) throws IOException {
			ringBuffer.write(data);
		}
		
	}
	
	private final static int BUFFER_SIZE = 1024;
	
	private byte[] ringBuffer = new byte[BUFFER_SIZE];
	private int readPos = 0;
	private int available = 0;
	private boolean releaseBuffer = false;

	private int writePos = 0;
	private int capacity = BUFFER_SIZE;
	
	private ReadFromRingBufferOutputStream inputStream;

	//-------------------------------------------------------------------
	/**
	 */
	public DataToTerminalInputStream() {
		inputStream = new ReadFromRingBufferOutputStream(this);
	}

	//-------------------------------------------------------------------
	public OutputStream getAsOutputStream() {
		return inputStream;
	}

	//-------------------------------------------------------------------
	public void write(int b) throws IOException {
//		logger.log(Logger.Level.INFO, "ENTER: write: b={0}", b);
		synchronized (ringBuffer) {
			// Wait until there is space in the buffer
			waitForCapacity(1);
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
	public void writeToTerminal(byte[] data) {
//		logger.log(Logger.Level.INFO, "ENTER: write: b={0}", data);
		synchronized (ringBuffer) {
			// Wait until there is space in the buffer
			waitForCapacity(data.length);
			if (capacity==0) {
				// Failed to write
				throw new RuntimeException("Buffer overflow: SIZE="+BUFFER_SIZE);
			}
			
			for (int i=0; i<data.length; i++) {
				ringBuffer[ writePos ] = data[i];
				writePos = (writePos + 1) % BUFFER_SIZE;  // Eventually wrap around
				available++;
				capacity--;
			}
			
			ringBuffer.notifyAll();
		}
//		logger.log(Logger.Level.INFO, "LEAVE: write: b={0}", data);
	}

	//-------------------------------------------------------------------
	private void waitForCapacity(int required) {
		synchronized (ringBuffer) {
			while (capacity < required) {
				try {
					ringBuffer.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
//		logger.log(Logger.Level.INFO, "ENTER: read");
		synchronized (ringBuffer) {
			while (available == 0) {
				try {
					ringBuffer.wait(100);
				} catch (InterruptedException e) {
				}
			}
			
			int b = ringBuffer[ readPos ] & 0xFF; // Convert to unsigned
			readPos = (readPos + 1) % BUFFER_SIZE; // Eventually wrap around
			available--;
			capacity++;
			
			ringBuffer.notifyAll();
			logger.log(Logger.Level.INFO, "LEAVE: read={0} / {1}", b&0xff,(char)b);
			return b;
		}
	}
	
	@Override
	public int read(byte[] buf) throws IOException {
//		logger.log(Logger.Level.INFO, "ENTER: read(byte[])");
		int len = 0;
		try {
			synchronized (ringBuffer) {
				// If we have read data, but there is none left at the moment, return
				if (available==0 && len>0) {
					return len;
				}
				while (available == 0) {
					try {
						ringBuffer.wait(100);
					} catch (InterruptedException e) {
					}
				}
				
				while (available>0 && len<buf.length) {
					buf[len++] = ringBuffer[ readPos ]; // Convert to unsigned
					readPos = (readPos + 1) % BUFFER_SIZE; // Eventually wrap around
					available--;
					capacity++;
				}
				
				ringBuffer.notifyAll();
				return len;
			}
		} finally {
			logger.log(Logger.Level.TRACE, "LEAVE: read(byte[]) returned {0} bytes", len);
		}
    }

	public void releaseBuffer() {
		// TODO Auto-generated method stub
		logger.log(Logger.Level.WARNING, "ENTER: releaseBuffer()");
		releaseBuffer = true;
	}}
