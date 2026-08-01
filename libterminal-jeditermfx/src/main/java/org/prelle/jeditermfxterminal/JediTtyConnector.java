package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.techsenger.jeditermfx.core.TtyConnector;

import lombok.Getter;
import lombok.Setter;

/**
 * This class maps read requests from the terminal to either
 */
class JediTtyConnector implements TtyConnector {

	private final static System.Logger logger = System.getLogger("jedi.terminal");
	/**
	 * Input from the MUD (read in an extra thread) is converted into the expected encoding 
	 * and written to this pipe, which is read by the terminal.
	 * When the client wants to directly write to the terminal, it can write into this writer too.
	 */
	private PipedWriter writeToTerminal;
	/**
	 * Used to access the data written to the pipe by the MUD or by the client. Called from 
	 * within the read method of the TtyConnector interface.
	 */
	private PipedReader readByTerminal;
	
	/**
	 * When the terminal emulator wants to respond to the server, it writes to this pipe, which is read by the MUD client.
	 */
	private PipedOutputStream writeToServer;
	/**
	 * Used to access the data written to the pipe by the terminal emulator. Called from 
	 * within the read method of the MUD client.
	 */
	private PipedInputStream readByServer;
	private Charset encoding = StandardCharsets.UTF_8;
	
	@Getter @Setter
	private boolean localEcho = true;

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 */
	public JediTtyConnector(Charset encoding) throws IOException {
		writeToTerminal = new PipedWriter();
		readByTerminal = new PipedReader(writeToTerminal);
		
		writeToServer = new PipedOutputStream();
		readByServer = new PipedInputStream(writeToServer);
		this.encoding = encoding;
	}

	@Override
	public int read(char[] buf, int offset, int length) throws IOException {
		logger.log(Level.TRACE, "read");
		return readByTerminal.read(buf, offset, length);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		logger.log(Level.TRACE, "write "+Arrays.toString(bytes));
		writeToServer.write(bytes);
		
//		if (localEcho) {
//			// Convert bytes to string using the specified encoding and write to terminal
//			String str = new String(bytes, encoding);
//			writeToTerminal.write(str);
//		}
	}

	// Write to terminal
	@Override
	public void write(String value) throws IOException {
//		logger.log(Level.INFO, "write "+value);
		// Convert string to bytes using the specified encoding
		writeToServer.write(value.getBytes(encoding));
		
		if (localEcho) {
			writeToTerminal.write(value);
		} else {
			writeToTerminal.write('*');;
		}
	}

	@Override
	public boolean isConnected() {
		logger.log(Level.INFO, "isConnected ");
		return true;
	}

	@Override
	public int waitFor() throws InterruptedException {
		logger.log(Level.INFO, "waitFor ");
		return 0;
	}

	@Override
	public boolean ready() throws IOException {
		logger.log(Level.TRACE, "ready ");
		return readByTerminal.ready();
	}

	@Override
	public String getName() {
		logger.log(Level.INFO, "getName ");
		return "MUD";
	}

	@Override
	public void close() {
		logger.log(Level.INFO, "close ");

	}

	//-------------------------------------------------------------------
	/**
	 * @return the writeToTerminal
	 */
	public PipedWriter getWriteToTerminal() {
		return writeToTerminal;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the readByServer
	 */
	public PipedInputStream getReadByServer() {
		return readByServer;
	}

}
