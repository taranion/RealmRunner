package org.prelle.realmrunner.web.views.play;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;

/**
 * 
 */
public class WebTerminal extends XTerm implements TerminalEmulator {
	
	private final static Logger logger = System.getLogger("terminal.web");
	
	private XTermOutputStream xout;
	private ANSIOutputStream out;
	
	private Thread readThread;

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
        setHeightFull();
        
        initComponents();
        initInteractivity();
	}

	//-------------------------------------------------------------------
	private void initComponents() {
    	xout = new XTermOutputStream(this);
    	out = new ANSIOutputStream(xout);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
    	addLineListener(ev->{
    	    String line = ev.getLine();
    	    logger.log(Level.INFO, "Input from xterm: "+line);
//    	    System.exit(1);
    	});	
		this.addCustomKeyListener( (de) -> logger.log(Level.INFO, "ARROW_UP"), Key.ARROW_UP);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getMode()
	 */
	@Override
	public TerminalMode getMode() {
		return TerminalMode.LINE_MODE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setMode(org.prelle.terminal.TerminalMode)
	 */
	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		// TODO Auto-generated method stub
		return this;
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
		return this;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getOutputStream()
	 */
	@Override
	public ANSIOutputStream getOutputStream() {
		return out;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getInputStream()
	 */
	@Override
	public ANSIInputStream getInputStream() {
		// TODO Auto-generated method stub
		return new ANSIInputStream(System.in);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getConsoleSize()
	 */
	@Override
	public int[] getConsoleSize() throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		return new int[] {80,40};
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

	//-------------------------------------------------------------------
	public void setUI(UI ui) {
		xout.setUI(ui);
	}

}
