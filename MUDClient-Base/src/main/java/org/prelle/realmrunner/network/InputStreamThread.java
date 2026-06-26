package org.prelle.realmrunner.network;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 */
public class InputStreamThread implements Runnable {
	
	private InputStream readFrom;
	private OutputStream writeTo;

//	//-------------------------------------------------------------------
//	public InputStreamThread(InputStream readFrom, OutputStream writeTo) {
//		this.readFrom = readFrom;
//		this.writeTo = writeTo;
//	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			int c;
			while ((c = readFrom.read()) != -1) {
				writeTo.write(c);
				writeTo.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
