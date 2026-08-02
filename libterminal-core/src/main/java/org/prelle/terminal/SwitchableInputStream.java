package org.prelle.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * Multiplexes incoming data from a primary InputStream source with locally injected data bytes.
 */
public class SwitchableInputStream extends InputStream {

	private final static Logger logger = System.getLogger("ghostty");

	private final List<Integer> injectedData = new ArrayList<>();
	private final Object lock = new Object();
	private volatile InputStream source;

	//-------------------------------------------------------------------
	@Override
	public String toString() {
		return "Switch <-+- " + source;
	}

	//-------------------------------------------------------------------
	@Override
	public int available() throws IOException {
		synchronized (lock) {
			return injectedData.size() + ((source != null) ? source.available() : 0);
		}
	}

	//-------------------------------------------------------------------
	public void setSource(InputStream source) {
		logger.log(Logger.Level.INFO, "Read from {0}", source);
		this.source = source;
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	//-------------------------------------------------------------------
	private void waitAMoment() throws IOException {
		synchronized (lock) {
			while (injectedData.isEmpty() && (source == null || source.available() == 0)) {
				try {
					lock.wait(50);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
	}

	//-------------------------------------------------------------------
	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		System.err.println("SwitchableInputStream.read(byte[],int,int) called");
		if (buf == null) throw new NullPointerException("Buffer cannot be null");
		if (off < 0 || len < 0 || len > buf.length - off) throw new IndexOutOfBoundsException();
		if (len == 0) return 0;

		while (true) {
			waitAMoment();

			// 1. First check if we have locally injected bytes
			synchronized (lock) {
				if (!injectedData.isEmpty()) {
					int count = Math.min(len, injectedData.size());
					for (int i = 0; i < count; i++) {
						buf[off + i] = (byte) (int) injectedData.remove(0);
					}
					// Print out the injected byte for debugging
					if (logger.isLoggable(Level.TRACE)) {
						String s = new String(buf, off, count);
						System.err.println("SwitchableInputStream: RCV "+s);
					}
					return count;
				}
			}

			// 2. Read from network source OUTSIDE of any synchronized lock block!
			InputStream src = this.source;
			if (src == null) {
				logger.log(Level.WARNING, "SwitchableInputStream: source is null");
				return -1;
			}

			int available = src.available();
			if (available == -1) {
				logger.log(Level.INFO, "SwitchableInputStream: source is closed");
				inject("Connection lost\r\n".getBytes());
				this.source = null;
				return 0;
			}

			System.err.println("SwitchableInputStream: RCV2.avail="+available);
			if (available > 0) {
				int bytesRead = src.read(buf, off, Math.min(len, available));
				if (bytesRead > 0 && logger.isLoggable(Level.INFO)) {
					String s = new String(buf, off, bytesRead);
					logger.log(Level.INFO, "RCV {0} bytes from source = {1}", bytesRead, s);
				}
				// Print out the injected byte for debugging
				if (logger.isLoggable(Level.INFO)) {
					String s = new String(buf, off, bytesRead);
					System.err.println("SwitchableInputStream: RCV2: "+s);
				}
				return bytesRead;
			}
		}
	}

	//-------------------------------------------------------------------
	@Override
	public int read(byte[] buf) throws IOException {
		System.err.println("SwitchableInputStream.read(byte[]) called");
		int t =  read(buf, 0, buf.length);
		// Debug string
		String s = new String(buf, 0, t);
		System.err.println("SwitchableInputStream.read(byte[]) returned "+s);
		return t;
	}

	//-------------------------------------------------------------------
	@Override
	public int read() throws IOException {
		System.err.println("SwitchableInputStream.read() called");
		while (true) {
			waitAMoment();
			synchronized (lock) {
				if (!injectedData.isEmpty()) {
					System.err.println("SwitchableInputStream.read(): "+ injectedData.get(0));
					return injectedData.remove(0);
				}
			}
			InputStream src = this.source;
			if (src != null && src.available() > 0) {
				System.err.println("SwitchableInputStream.read(): ?");
				return src.read();
			}
		}
	}

	//-------------------------------------------------------------------
	public void inject(byte[] data) {
		if (data == null || data.length == 0) return;
		inject(data, 0, data.length);
	}

	//-------------------------------------------------------------------
	public void inject(int data) {
		synchronized (lock) {
			injectedData.add(data & 0xFF);
			lock.notifyAll();
		}
	}

	//-------------------------------------------------------------------
	public void inject(byte[] b, int off, int len) {
		if (b == null || len <= 0) return;
		synchronized (lock) {
			int maxLen = Math.min(len, b.length - off);
			for (int i = 0; i < maxLen; i++) {
				injectedData.add((int) b[off + i] & 0xFF);
			}
			lock.notifyAll();
		}
	}
}
