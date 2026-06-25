package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;

/**
 * 
 */
public class SwitchableOutputStream extends OutputStream {
	
	private OutputStream sink;

	//-------------------------------------------------------------------
	public SwitchableOutputStream() {
		
	}

	//-------------------------------------------------------------------
	/**
	 * @param sink the sink to set
	 */
	public void setSink(OutputStream sink) {
		this.sink = sink;
		System.getLogger("ghostty").log(System.Logger.Level.INFO, "SwitchableOutputStream.setSink({0})", sink);
	}

	@Override
	public void write(int b) throws IOException {
		System.getLogger("ghostty").log(System.Logger.Level.INFO, String.valueOf((char)b));
		if (sink!=null) sink.write(b);
	}

}
