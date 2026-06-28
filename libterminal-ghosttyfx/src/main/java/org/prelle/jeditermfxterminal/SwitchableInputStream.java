package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.lang.System.Logger;

/**
 * 
 */
public class SwitchableInputStream extends InputStream {
	
	private final static Logger logger = System.getLogger("ghostty");
	
	private InputStream source;

	//-------------------------------------------------------------------
	public SwitchableInputStream() {
//		source = new StringBufferInputStream("Greeting!\r\nHello World");
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
	/**
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		logger.log(Logger.Level.INFO, "ENTER: read()");
		while (source==null || source.available()==0) {
			synchronized (this) {
				try {
					wait(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (source==null) {
			System.err.println("SwitchableInputStream: source is null");
			System.exit(1);
			return -1;
		}
		int c = source.read();
//		logger.log(Logger.Level.INFO, "read : {0}", (char)c);
		System.out.print( (char)c);
		return c;
	}

	
	//-------------------------------------------------------------------
	/**
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] buf) throws IOException {
		logger.log(Logger.Level.TRACE, "ENTER: read(byte[])");
		try {
			while (source==null || source.available()==0) {
//				logger.log(Logger.Level.INFO, "ENTER: read(byte[]) avail="+((source!=null)?source.available():null));
				synchronized (this) {
					try {
						wait(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			if (source==null) {
				System.err.println("SwitchableInputStream: source is null");
				System.exit(1);
				return -1;
			}
			int c = source.read(buf);
//			logger.log(Logger.Level.INFO, "read : {0}", new String(buf, 0, c));
			return c;
		} catch (Exception e) {
			logger.log(Logger.Level.ERROR, "Exception in read(byte[]) : {0}", e);
			throw e;
		} finally {
			logger.log(Logger.Level.TRACE, "LEAVE: read(byte[]) ");
		}
	}
	
}
