package org.prelle.terminal.emulated.delete;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.terminal.emulated.TerminalController;
import org.prelle.terminal.emulated.TerminalModel;

/**
 * Create a controller that requires write operations called directly
 */
public class PassiveController extends TerminalController {

	private final static Logger logger = System.getLogger(PassiveController.class.getPackageName());

	//-------------------------------------------------------------------
	public PassiveController(TerminalModel model, Emulation emul) {
		super(model, emul);
	}

	//-------------------------------------------------------------------
	public void write(String text) {
		ByteArrayInputStream bain = new ByteArrayInputStream(text.getBytes(Charset.defaultCharset()));
		try (ANSIInputStream in = new ANSIInputStream(bain)) {
			while (true) {
				AParsedElement elem = in.readFragment();
				if (elem==null)
					break;
				logger.log(Level.TRACE, "Write {0}", elem);
				handleFragment(elem);
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, "Error reading",e);
		}
	}

}
