package org.prelle.ghostty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.github.vlaaad.ghosttyfx.Terminal;

/**
 * This class maps read requests from the terminal to either
 */
class GhosttyTerminalConnector implements Terminal {

	private final static System.Logger logger = System.getLogger("jedi.terminal");
	/**
	 * Input from the MUD (read in an extra thread) is converted into the expected encoding 
	 * and written to this pipe, which is read by the terminal.
	 * When the client wants to directly write to the terminal, it can write into this writer too.
	 */
	private PipedOutputStream writeToTerminal;
	/**
	 * Used to access the data written to the pipe by the MUD or by the client. Called from 
	 * within the read method of the TtyConnector interface.
	 */
	private PipedInputStream readByTerminal;
	
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

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 */
	public GhosttyTerminalConnector(Charset encoding) throws IOException {
		writeToTerminal = new PipedOutputStream();
		readByTerminal = new PipedInputStream(writeToTerminal);
		
		writeToServer = new PipedOutputStream();
		readByServer = new PipedInputStream(writeToServer);
		this.encoding = encoding;
	}

    /// The terminal view reads this stream and writes the received bytes to the
    /// terminal emulator.
    ///
    /// @return the stream that produces terminal output
    /// @throws Exception if the output stream cannot be opened
	@Override
	public InputStream output() throws Exception {
		return readByTerminal;
	}

	//-------------------------------------------------------------------
	/**
	 *  The terminal view writes encoded keyboard input, paste data, and other
	 *  terminal input bytes to this stream.
     *
     * @return the stream that accepts terminal input
     * @throws Exception if the input stream cannot be opened
	 * @see io.github.vlaaad.ghosttyfx.Terminal#input()
	 */
	@Override
	public OutputStream input() throws Exception {
		return writeToServer;
	}

	//-------------------------------------------------------------------
	/**
	 * @see io.github.vlaaad.ghosttyfx.Terminal#close()
	 */
	@Override
	public void close() throws Exception {
		logger.log(Level.WARNING, "close");
	}

	//-------------------------------------------------------------------
	/**
	 * @return the writeToTerminal
	 */
	public PipedOutputStream getWriteToTerminal() {
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
