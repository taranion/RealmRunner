package org.prelle.realmrunner.network;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.prelle.ansi.ANSIInputStreamFilter;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.ControlSequenceFragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.SelectGraphicRendition;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetListener;
import org.prelle.telnet.TelnetSubnegotiationHandler;
import org.prelle.telnet.option.MXPOption;

/**
 * 
 */
public class MXPInputStreamFilter implements ANSIInputStreamFilter, TelnetListener {
	
	private static enum MXPDefinition {
		BR((cmd) -> new C0Fragment(C0Code.CR), (cmd)->null, "BR"),
		BOLD((cmd) -> new SelectGraphicRendition(1), (cmd)->new SelectGraphicRendition(23), "B", "BOLD","STRONG"),
		ITALIC((cmd) -> new SelectGraphicRendition(3), (cmd)->new SelectGraphicRendition(23), "I", "ITALIC","EM"),
		UNDERLINE((cmd) -> new SelectGraphicRendition(4), (cmd)->new SelectGraphicRendition(21), "U", "UNDERLINE"),
		STRIKEOUT((cmd) -> new SelectGraphicRendition(9), (cmd)->new SelectGraphicRendition(29), "S", "STRIKEOUT"),
		;
		private MXPCommand onCommand, offCommand;
		private List<String> names;
		MXPDefinition(MXPCommand cmdOn, MXPCommand cmdOff, String... args) {
			this.onCommand = cmdOn;
			this.offCommand = cmdOff;
			this.names = List.of(args);
		}
	}
	
	private static enum MXPMode {
		/** Only MXP commands in the "open" category are allowed. */
		OPEN,
		/** All tags and commands in MXP are allowed */
		SECURE,
		/** no MXP or HTML commands are allowed */
		LOCKED
	}
	
	private static enum MXPUntil {
		NEXT_TAG,
		LINE,
		LOCKED
	}
	

	private static enum MXPTag {
		
		OPEN_LINE(0),
		SECURE_LINE(1),
		LOCKED_LINE(2),
		RESET(3),
		SECURE_TEMP(4),
		OPEN_LOCKED(5),
		SECURE_LOCKED(6),
		LOCK_LOCKED(7),
		ROOM_NAME(10)
		;
		private int value;
		MXPTag(int val) {
			this.value=val;
		}
		public static MXPTag modeOf(int val) {
			for (MXPTag m: MXPTag.values()) {
				if (m.value==val) return m;
			}
			return null;
		}
	}

	private final static Logger logger = System.getLogger(MXPInputStreamFilter.class.getPackageName());
	
	private boolean mxpActive = false;
	private MXPMode defaultMode = MXPMode.OPEN;
	private MXPMode currentMode = MXPMode.OPEN; // May also be OPEN 
	private MXPUntil duration = null;

	private StringBuilder dtd = new StringBuilder();
	
	private transient MXPOption mxpOption;
	
	//-------------------------------------------------------------------
	/**
	 * @param mxp 
	 */
	public MXPInputStreamFilter(MXPOption mxp) {
		this.mxpOption = mxp;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.ansi.ANSIInputStreamFilter#handles(org.prelle.ansi.AParsedElement)
	 */
	@Override
	public boolean handles(AParsedElement event) {
//		logger.log(Level.DEBUG, "call handles("+event+")");
		if (!mxpActive) return false;
		// Handle MXP tags
		if (event instanceof ControlSequenceFragment csi && csi.getFinalChar()=='z') {
			return true;
		}
		if (event instanceof PrintableFragment) {
			// Handle printable content based on current mode
			return currentMode!=MXPMode.LOCKED;
		} else if (event instanceof C0Fragment c0) {
			// If this is a newline and the mode was just valued for the line, reset to default mode
			if (c0.getCode()==C0Code.CR || c0.getCode()==C0Code.LF) {
				if (duration==MXPUntil.LINE) {
					currentMode = defaultMode;
					logger.log(Level.INFO, "Return mode to "+defaultMode+" after line break");
					duration = null;
				}
			}
			return false;
		}
		return false;
	}
	
	//-------------------------------------------------------------------
	private void processTag(int tag ) {
		MXPTag mxpTag = MXPTag.modeOf(tag);
		if (mxpTag==null) {
			logger.log(Level.WARNING, "Unknown MXP tag {0}", tag);
			return;
		}
		switch (mxpTag) {
		case OPEN_LINE:
			currentMode = MXPMode.OPEN;
			duration    = MXPUntil.LINE;
			break;
		case SECURE_LINE:
			currentMode = MXPMode.SECURE;
			duration    = MXPUntil.LINE;
			break;
		case LOCKED_LINE:
			currentMode = MXPMode.LOCKED;
			duration    = MXPUntil.LINE;
			break;
		case RESET:
			// close all open tags. Set mode to Open. Set text color and properties to default.
			currentMode = MXPMode.OPEN;
			duration    = MXPUntil.LINE;
			break;
		case SECURE_TEMP:
			currentMode = MXPMode.SECURE;
			duration    = MXPUntil.NEXT_TAG;
			break;
		case OPEN_LOCKED:
			currentMode = MXPMode.OPEN;
			defaultMode = MXPMode.LOCKED;
			duration    = MXPUntil.LOCKED;
			break;
		case SECURE_LOCKED:
			currentMode = MXPMode.SECURE;
			defaultMode = MXPMode.LOCKED;
			duration    = MXPUntil.LOCKED;
			break;
		case LOCK_LOCKED:
			currentMode = MXPMode.LOCKED;
			defaultMode = MXPMode.LOCKED;
			duration    = MXPUntil.LOCKED;
			break;
		case ROOM_NAME:
			logger.log(Level.INFO, "MXP ROOM_NAME tag received");
			break;
		default:
			logger.log(Level.WARNING, "Unhandled MXP tag {0}", mxpTag);
		}
		
		logger.log(Level.INFO, "MXP tag {0} processed. Current mode: {1}, default mode: {2}, duration: {3}", mxpTag, currentMode, defaultMode, duration);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.ansi.ANSIInputStreamFilter#process(org.prelle.ansi.AParsedElement)
	 */
	@Override
	public List<AParsedElement> process(AParsedElement event) {
		if (event instanceof ControlSequenceFragment csi && csi.getFinalChar()=='z') {
			int tag = csi.getArguments().isEmpty()? 0 : csi.getArguments().get(0);
			processTag(tag);
			return List.of();
		}
//		logger.log(Level.WARNING, "Check for MXP in "+event);
		if (event instanceof PrintableFragment print) {
			String text = print.getText();
//			if (text.startsWith("<!")) {
//				dtd.append(text+"\n");
//				return List.of();
//			}
			if (text.contains("<"))
				return convertIntoElements(text);
		}
		return List.of(event);
	}
	
	

	//-------------------------------------------------------------------
	private List<AParsedElement> convertIntoElements(String text) {
		List<AParsedElement> ret = new ArrayList<>();
		boolean caretOpen = false;
		boolean quoted = false;
		StringBuilder sb = new StringBuilder();
		for (char c: text.toCharArray()) {
			if (c=='<' && !quoted) {
				if (caretOpen) {
					sb.append(c);
				} else {
					caretOpen = true;
					// If there is any text before the caret, add it as a PrintableFragment
					if (sb.length()>0) {
						ret.add(new PrintableFragment(sb.toString()).setRaw(sb.toString().getBytes()));
						sb.setLength(0);
					}
				}
			} else if (c=='>' && !quoted) {
				if (caretOpen) {
					caretOpen = false;
					processMXPCommand(sb.toString()).ifPresent(toAdd -> ret.add(toAdd));
					sb.setLength(0);
				} else {
					sb.append(c);
				}
			} else {
				if (caretOpen && c=='\'') {
					quoted = !quoted;
				}
				sb.append(c);
			}
		}
		if (sb.length()>0) {
			ret.add(new PrintableFragment(sb.toString()).setRaw(sb.toString().getBytes()));
			sb.setLength(0);
		}
		return ret;
	}

	private Optional<AParsedElement> processMXPCommand(String value) {
		logger.log(Level.INFO, "processMXPCommand: "+value);
		for (MXPDefinition def: MXPDefinition.values()) {
			for (String name: def.names) {
				if (value.equalsIgnoreCase(name)) {
					System.err.println("MXPInputStreamFilter: Replace "+value+" with "+def.onCommand.apply(value));
					return Optional.ofNullable(def.onCommand.apply(value));
				} else if (value.equalsIgnoreCase("/"+name)) {
					return Optional.ofNullable(def.offCommand.apply(value));
				}
			}
		}
		if (value.toUpperCase().startsWith("!")) {
			dtd.append('<'+value+">\n");
			mxpOption.fireDTDChange(dtd.toString());
		} else {
			logger.log(Level.WARNING, "Unknown MXP: {0}", value);
		}
		return Optional.empty();
	}

	//-------------------------------------------------------------------
	public void setMXPActive(boolean active) {
		this.mxpActive = active;
	}

	//-------------------------------------------------------------------
	public String getDTD() {
		return dtd.toString();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetListener#optionStateChanged(org.prelle.telnet.TelnetSubnegotiationHandler, boolean)
	 */
	@Override
	public void optionStateChanged(TelnetSubnegotiationHandler extension, boolean active) {
		if (extension instanceof MXPOption) {
			logger.log(Level.INFO, "MXP option state changed: {0}", active);
			setMXPActive(active);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetListener#telnetCommandReceived(org.prelle.telnet.TelnetCommand)
	 */
	@Override
	public void telnetCommandReceived(TelnetCommand command) {
		// TODO Auto-generated method stub
		
	}

	
	
}
