package org.prelle.realmrunner.web.views.play;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.lang.System.Logger;
import java.util.function.BiConsumer;

import org.prelle.ansi.ANSIOutputStream;

/**
 * 
 */
public class WebTermANSIOutputStream extends ANSIOutputStream {
	
	private final static Logger logger = System.getLogger("terminal.web");
	
	private WebTerminal term;
	private BiConsumer<String,String> loggingListener;

	//-------------------------------------------------------------------
	/**
	 * @param out
	 */
	public WebTermANSIOutputStream(WebTerminal term) {
		super(System.out);
		this.term = term;
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.FilterOutputStream#write(int)
	 */
	public synchronized void write(int value) throws IOException {
		String str = "\u001B"+(char)(value-64);
		System.err.println("Wrote1: "+str);
		term.write(str);
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.FilterOutputStream#write(byte[])
	 */
	public synchronized void write(byte[] values) throws IOException {
		System.err.println("Wrote2: "+new String(values));
		term.write(new String(values));
	}

	//-------------------------------------------------------------------
	public synchronized void write(String value) throws IOException {
		System.err.println("Wrote3: "+value);
		term.write(value);
	}

}
