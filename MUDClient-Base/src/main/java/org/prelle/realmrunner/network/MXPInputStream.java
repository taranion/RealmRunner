package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.MXPLine;

/**
 *
 */
public class MXPInputStream extends ANSIInputStream {
	
	private final static Logger logger = System.getLogger("mud.mxp");
	
	private static enum MXPMode {
		/** only MXP commands in the "open" category are allowed. */
		OPEN,
		/** all tags and commands in MXP are allowed */
		SECURE,
		/** no MXP or HTML commands are allowed in the line.  */
		LOCKED,
		;
	}	
	private MXPMode defaultMode = MXPMode.OPEN;
	private MXPMode currentMode = defaultMode;
	private boolean modeEndsOnNewLine = true;
	private StringBuffer mxpBuffer = new StringBuffer();

	/** Elements waiting to be returned */
	private List<AParsedElement> queue = new ArrayList<AParsedElement>();

	//-------------------------------------------------------------------
	public MXPInputStream(InputStream in) {
		super(in);
	}
	
	//-------------------------------------------------------------------
	private void consumeMXPLine(MXPLine mxp) {
//		logger.log(Level.INFO, "consumeMXPLine "+mxp.getInterpretedValue());
		switch (mxp.getInterpretedValue()) {
		case OPEN_LINE:
			currentMode = MXPMode.OPEN;
			modeEndsOnNewLine = true;
			break;
		case SECURE_LINE:
			currentMode = MXPMode.SECURE;
			modeEndsOnNewLine = true;
			break;
		case LOCKED_LINE:
			currentMode = MXPMode.LOCKED;
			modeEndsOnNewLine = true;
			break;
		case SECURE_TEMP:
			currentMode = MXPMode.SECURE;
			modeEndsOnNewLine = false;
			break;
		case OPEN_LOCK:
			currentMode = MXPMode.OPEN;
			defaultMode = MXPMode.OPEN;
			modeEndsOnNewLine = false;
			break;
		case SECURE_LOCK:
			currentMode = MXPMode.SECURE;
			defaultMode = MXPMode.SECURE;
			modeEndsOnNewLine = false;
			break;
		case LOCKED_LOCK:
			currentMode = MXPMode.LOCKED;
			defaultMode = MXPMode.LOCKED;
			modeEndsOnNewLine = false;
			break;
		}
		logger.log(Level.INFO, "Mode is now {0}", currentMode);
	}
	
	//-------------------------------------------------------------------
	public AParsedElement readFragment() throws IOException {
		while (true) {
			if (!queue.isEmpty()) {
				AParsedElement frag = queue.remove(0);
				logger.log(Level.ERROR, "Read queued "+frag);
				return frag;
			}

			AParsedElement frag = super.readFragment();
			//logger.log(Level.INFO, "Read "+frag);
			if (frag==null)
				return null;
			switch (frag) {
			case MXPLine mxp -> {
				consumeMXPLine(mxp);
				continue;
			}
			case C0Fragment c0 when (c0.getCode()==C0Code.CR) -> {
				if (currentMode==MXPMode.OPEN || currentMode==MXPMode.LOCKED) {
					parseLine(mxpBuffer.toString());
					mxpBuffer.delete(0, mxpBuffer.length());
				}
				if (modeEndsOnNewLine && currentMode!=defaultMode) {
					currentMode = defaultMode;
					logger.log(Level.INFO, "Mode is now {0}", currentMode);
				}
				parseLine(mxpBuffer.toString());
			}
			case PrintableFragment print -> {
				String text = print.getText();
//				logger.log(Level.INFO, "Handle printable in mode "+currentMode+": "+text);
				// If we are in MXP mode, everything is added to the MXP
				// buffer (until end of line)
				if (currentMode==MXPMode.OPEN || currentMode==MXPMode.SECURE) {
					parseLine(text);
//					mxpBuffer.append(text);
					continue;
				}
			}
			default -> {
				break;
			}
			}
			return frag;
		}
	}
	
	//-------------------------------------------------------------------
	private void parseLine(String line) {
		logger.log(Level.INFO, "Check for MXP: {0}", line);
		List<AParsedElement> result = new ArrayList<>();
		boolean tagOpen = false;
		PrintableFragment buffer = new PrintableFragment();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c=='<') {
				if (tagOpen) {
					logger.log(Level.WARNING, "Parse error: Position {0} has a < although tag is already open", i);
					if (!buffer.isEmpty())
						queue.add(buffer);
					return;
				} else {
					tagOpen=true;
				}
			} else if (c=='>') {
				if (tagOpen) {
					String tagName = buffer.getText();
					buffer.clear();
					if (tagName.startsWith("/")) {
						logger.log(Level.DEBUG, "End tag: {0}",tagName);
						result.add(new MXPEndTag(tagName));
					} else if (tagName.endsWith("/")) {
						logger.log(Level.DEBUG, "Single tag: {0}",tagName);
						result.add(new MXPSingleTag(tagName));
					} else {
						logger.log(Level.DEBUG, "Start tag: {0}",tagName);
						result.add(new MXPStartTag(tagName));
					}
					tagOpen=false;
					// Does the mode ends now or at the end of the line?
					if (!modeEndsOnNewLine) {
						// Mode was only set for this tag
						logger.log(Level.INFO, "mode was only temporary - switch back to default {0}", currentMode);
						currentMode = defaultMode;
					}
				} else {
					buffer.add(c);
				}
			} else {
				buffer.add(c);
			}
		}
		
//		if (!tagOpen) {
			result.add(buffer);
//		}
		queue.addAll(result);
	}

}
