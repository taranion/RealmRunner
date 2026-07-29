package org.prelle.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
	public String toString() {
		return "Switch <-+- "+source;
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
			try {
//			System.err.println("SwitchableInputStream.read(byte[]): waiting for data");
			waitAMoment();
			// If there are injected data return as many as possible into the buffer
			synchronized (injectedData) {
				if (injectedData.size() > 0) {
					int len = Math.min(buf.length, injectedData.size());
					for (int i=0; i<len; i++) {
						buf[i] = (byte)(int)injectedData.remove(0);
					}
					System.err.println("SwitchableInputStream.read(byte[]): reading "+len+" injected");
					return len;
				}
			}
			if (source==null) {
				System.err.println("SwitchableInputStream: source is null");
				System.exit(1);
				return -1;
			}
			//System.err.println("SwitchableInputStream: call available on "+source);
			int available = source.available();
			logger.log(Level.INFO,"  available() returned "+available);
			if (available==-1) {
				System.err.println("SwitchableInputStream: source is closed");
				inject("Connection lost\r\n".getBytes());
				source = null;
				return 0;
			}
			if (available>0) {
				System.err.println("SwitchableInputStream.read(byte[]): reading "+available+" from "+source);
//				int toRead = Math.min(buf.length, available);
				if (available>100) {
					System.err.println("SwitchableInputStream:Debug");
				}
				int len = source.read(buf);
				System.err.println("SwitchableInputStream.read(byte[]): done reading with "+len);
				//Convert read to String
				if (logger.isLoggable(Level.INFO)) {
					String s = new String(buf, 0, len);
					logger.log(Level.INFO, "RCV {0} bytes from source = {1}", len, s);
				}
				if (len>0 || source.available()==0) 
					return len;
			}
			} catch (Exception e) {
				logger.log(Level.ERROR, "SwitchableInputStream.read(byte[]): Exception: {0}", e);
				throw e;
			}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		logger.log(Logger.Level.INFO, "ENTER: read()");
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
		synchronized (injectedData) {
			for (byte b : data) {
				injectedData.add((int)b);
			}
			injectedData.notifyAll();
		}
	}
	
	//-------------------------------------------------------------------
	public void inject(int data) {
		logger.log(Level.WARNING, "Injecting single byte {0}", (char)data);
		synchronized (injectedData) {
			injectedData.add(data);
			injectedData.notifyAll();
		}
	}

	public void inject(byte[] b, int off, int len) {
		logger.log(Level.WARNING, "Injecting {0} bytes", b.length);
		synchronized (this) {
			int i= 0;
			int maxLen = Math.min(len, b.length-off);
			for (; i< maxLen; i++) {
				injectedData.add((int)b[off+i]);
			}
			injectedData.notifyAll();
		}
	}
	
}
