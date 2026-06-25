package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.lang.System.Logger.Level;

import io.github.vlaaad.ghosttyfx.Terminal;

/**
 *
 */
public class RemoteGhosttyTerminal implements Terminal {

	private final static System.Logger logger = System.getLogger("ghostty.terminal");


	private OutputStream out;
	private InputStream in;
	
	public RemoteGhosttyTerminal(InputStream inPipe, OutputStream outPipe) {
		this.out = outPipe;
		this.in  = inPipe;
	}

	@Override
	public InputStream output() throws Exception {
		logger.log(Level.WARNING, "ENTER: output()");
		return in;
	}

	@Override
	public OutputStream input() throws Exception {
		logger.log(Level.WARNING, "ENTER: input()");
		// TODO Auto-generated method stub
		return out;
	}

	@Override
	public void resize(int columns, int rows) throws Exception {
		logger.log(Level.WARNING, "ENTER: resize({0},{1})", columns, rows);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws Exception {
		logger.log(Level.WARNING, "ENTER: close()");
		// TODO Auto-generated method stub
		
	}

}
