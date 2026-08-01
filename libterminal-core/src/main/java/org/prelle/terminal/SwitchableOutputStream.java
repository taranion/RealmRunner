package org.prelle.terminal;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Switchable output stream wrapper forwarding writes to a dynamic sink OutputStream.
 */
public class SwitchableOutputStream extends OutputStream {

	private OutputStream sink;

	//-------------------------------------------------------------------
	public SwitchableOutputStream() {
	}

	//-------------------------------------------------------------------
	@Override
	public String toString() {
		return "Switch --> " + sink;
	}

	//-------------------------------------------------------------------
	public void setSink(OutputStream sink) {
		this.sink = sink;
	}

	//-------------------------------------------------------------------
	public OutputStream getSink() {
		return sink;
	}

	//-------------------------------------------------------------------
	@Override
	public void write(int b) throws IOException {
		if (sink != null) {
			sink.write(b);
		}
	}

	//-------------------------------------------------------------------
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (sink != null) {
			sink.write(b, off, len);
		}
	}

	//-------------------------------------------------------------------
	@Override
	public void write(byte[] b) throws IOException {
		if (sink != null) {
			sink.write(b, 0, b.length);
		}
	}
}
