package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;

/**
 * 
 */
public class SwitchableInputStream extends InputStream {
	
	private InputStream source;

	//-------------------------------------------------------------------
	public SwitchableInputStream() {
		source = new StringBufferInputStream("Greeting!\r\nHello World");
	}

	//-------------------------------------------------------------------
	public void setSource(InputStream source) {
		this.source = source;
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		return source.read();
	}

}
