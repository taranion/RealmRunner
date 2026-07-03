package org.prelle.jeditermfxterminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class SwitchableInputStream extends InputStream {
	
	private final static Logger logger = System.getLogger("ghostty");
	
	private List<Integer> injectedData = new ArrayList<>();
	private InputStream source;

	//-------------------------------------------------------------------
	public SwitchableInputStream() {
//		source = new StringBufferInputStream("Greeting!\r\nHello World");
	}

	//-------------------------------------------------------------------
	public String toString() {
		return "Switch <-- "+source;
	}

    //-------------------------------------------------------------------
	public int available() throws IOException {
		return injectedData.size() + ((source!=null)?source.available():0);
	}

	//-------------------------------------------------------------------
	public void setSource(InputStream source) {
		logger.log(Logger.Level.INFO, "Read from {0}", source);
		this.source = source;
		synchronized (this) {
			notifyAll();
		}
	}

	//-------------------------------------------------------------------
	private void waitAMoment() throws IOException {
		while (injectedData.isEmpty() && (source==null || source.available()==0)) {
			synchronized (this) {
				try {
					wait(50);
//					System.getLogger("ghostty").log(System.Logger.Level.INFO, "Wait for more data or a new source");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//-------------------------------------------------------------------
	public int read(byte[] buf) throws IOException {
		while (true) {
//			System.err.println("SwitchableInputStream.read(byte[]): waiting for data");
			waitAMoment();
			// If there are injected data return as many as possible into the buffer
			if (injectedData.size() > 0) {
				int len = Math.min(buf.length, injectedData.size());
				for (int i=0; i<len; i++) {
					buf[i] = (byte)(int)injectedData.remove(0);
				}
//				System.err.println("SwitchableInputStream.read(byte[]): reading "+len+" injected");
				return len;
			}
			if (source==null) {
				System.err.println("SwitchableInputStream: source is null");
				System.exit(1);
				return -1;
			}
			if (source.available()>0) {
//				System.err.println("SwitchableInputStream.read(byte[]): reading "+source.available()+" from source");
				int len = source.read(buf,0,source.available());
//				// Convert read to String
//				String s = new String(buf, 0, len);
//				System.err.println("SwitchableInputStream.read(byte[]): "+s);
				return len;
			}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
//		logger.log(Logger.Level.INFO, "ENTER: read()");
		do {
			waitAMoment();
			if (!injectedData.isEmpty()) {
				int c = injectedData.remove(0);
				logger.log(Logger.Level.INFO, "read : {0}", (char)c);
				return c;
			}
			if (source!=null) {
				int c = source.read();
				logger.log(Logger.Level.INFO, "read : {0}", (char)c);
//				System.out.print( (char)c);
				return c;
			}
			// Wait for more data or a new source
		} while (true);
	}
	
	//-------------------------------------------------------------------
	public void inject(byte[] data) {
		logger.log(Level.WARNING, "Injecting {0} bytes", data.length);
		for (byte b : data) {
			injectedData.add((int)b);
		}
		synchronized (this) {
			notifyAll();
		}
	}

	
//	//-------------------------------------------------------------------
//	/**
//	 * @see java.io.InputStream#read(byte[])
//	 */
//	@Override
//	public int read(byte[] buf) throws IOException {
//		logger.log(Logger.Level.TRACE, "ENTER: read(byte[])");
//		try {
//			while (source==null || source.available()==0) {
////				logger.log(Logger.Level.INFO, "ENTER: read(byte[]) avail="+((source!=null)?source.available():null));
//				synchronized (this) {
//					try {
//						wait(50);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//			if (source==null) {
//				System.err.println("SwitchableInputStream: source is null");
//				System.exit(1);
//				return -1;
//			}
//			int c = source.read(buf);
////			logger.log(Logger.Level.INFO, "read : {0}", new String(buf, 0, c));
//			return c;
//		} catch (SocketTimeoutException e) {
//			logger.log(Logger.Level.ERROR, "Timeout");
//			throw e;
//		} catch (Exception e) {
//			logger.log(Logger.Level.ERROR, "Exception in read(byte[]) : "+e.toString());
//			throw e;
//		} finally {
//			logger.log(Logger.Level.TRACE, "LEAVE: read(byte[]) ");
//		}
//	}
	
}
