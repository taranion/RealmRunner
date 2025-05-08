package org.prelle.realmrunner.web.views.play;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import com.flowingcode.vaadin.addons.xterm.XTerm;

/**
 * 
 */
public class WebTerminal extends XTerm implements TerminalEmulator {
	
	private final static Logger logger = System.getLogger("terminal.web");

	//-------------------------------------------------------------------
	/**
	 */
	public WebTerminal() {
    	setCursorBlink(true);
    	setCursorStyle(CursorStyle.UNDERLINE);
    	setCopySelection(true);
        setUseSystemClipboard(UseSystemClipboard.READWRITE);
        setPasteWithMiddleClick(true);
        setPasteWithRightClick(true);
        
        initInteractivity();
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
    	addLineListener(ev->{
    	    String line = ev.getLine();
    	    logger.log(Level.INFO, "Input from xterm: "+line);
    	});	
		
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getMode()
	 */
	@Override
	public TerminalMode getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setMode(org.prelle.terminal.TerminalMode)
	 */
	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#isLocalEchoActive()
	 */
	@Override
	public boolean isLocalEchoActive() {
		// TODO Auto-generated method stub
		return false;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setLocalEchoActive(boolean)
	 */
	@Override
	public TerminalEmulator setLocalEchoActive(boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getOutputStream()
	 */
	@Override
	public ANSIOutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getInputStream()
	 */
	@Override
	public ANSIInputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getConsoleSize()
	 */
	@Override
	public int[] getConsoleSize() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getEncodings()
	 */
	@Override
	public Charset[] getEncodings() {
		// TODO Auto-generated method stub
		return null;
	}

}
