package org.prelle.realmrunner.network;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.function.Function;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.MXPLine;
import org.prelle.ansi.control.AreaControls;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetInputStream;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSocket.State;
import org.prelle.telnet.mud.MUDSoundProtocolListener;
import org.prelle.telnet.TelnetSocketListener;

/**
 *
 */
public class ReadFromMUDTask implements Runnable, TelnetSocketListener {

	private final static Logger logger = System.getLogger("mud.client");

	private TelnetSocket mud;
	private ANSIOutputStream forwardTo;
	private AbstractConfig config;
	private Charset encoding;
	private boolean inMXPMode;
	private boolean inMSPMode;
	private StringBuffer lineBuffer = new StringBuffer();
	private StringBuffer mxpBuffer = new StringBuffer();

	private Function<AParsedElement, AParsedElement> controlSequenceFilter;

	//-------------------------------------------------------------------
	public ReadFromMUDTask(TelnetSocket mud, ANSIOutputStream forwardTo, AbstractConfig config, Charset defaultEncoding) {
		this.mud = mud;
		this.forwardTo = forwardTo;
		this.config    = config;
		this.encoding  = defaultEncoding;
		
		mud.addSocketListener(this);
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			TelnetInputStream in = (TelnetInputStream)mud.getInputStream();
			ANSIInputStream ain = new ANSIInputStream(in);
			ain.setEncoding(encoding);
			ain.setCollectPrintable(false);
			ain.setLoggingListener( (type,text) -> {
				if (!"PRINTABLE".equals(type)) {
					if (text.startsWith("C0(CR") || text.startsWith("C0(LF")) return;
					if (text.startsWith("SGR")) return;
					logger.log(Level.INFO, "MUD <-- {0} = {1}", type,text);
				}
			});
			while (true) {
				AParsedElement frag = ain.readFragment();
				if (frag==null) {
					logger.log(Level.WARNING, "Connection to MUD lost");
					AreaControls.clearScreen(forwardTo);
					forwardTo.reset();
					forwardTo.flush();
					System.exit(0);
					return;
				}
				logger.log(Level.TRACE, "MUD: {0}={1}  forward={2}", frag.getName(), frag.toString(), forwardTo);

				switch (frag) {
				case C0Fragment c0 -> {
					// If we have been in MXP mode so far, leave it now
					if (inMXPMode) {
						inMXPMode=false;
						receivedMXP(mxpBuffer.toString());
						mxpBuffer.delete(0, mxpBuffer.length());
					} else if (c0.getCode()==C0Code.CR) {
						// Line terminated
						String line = lineBuffer.toString();
						lineBuffer.delete(0, lineBuffer.length());
						if (inMSPMode && line.startsWith("!!")) {
							// This was an MSP command
							receivedMSP(line);
							// Don't process the input any further
							continue;
						}
					}
				}
				case MXPLine mxp -> {
					if (inMXPMode) {
						logger.log(Level.WARNING, "TODO: MXP: "+mxpBuffer);
					} else
						logger.log(Level.WARNING, "MXP has been announced");
					inMXPMode=true;
					mxpBuffer.delete(0, mxpBuffer.length());
					continue;
				}
				case PrintableFragment print -> {
					String text = print.getText();
					// If we are in MXP mode, everything is added to the MXP
					// buffer (until end of line)
					if (inMXPMode) {
						mxpBuffer.append(text);
						continue;
					}
					if (inMSPMode && ")".equals(text)) {
						// MSP command ending
						lineBuffer.append(")");
						String line = lineBuffer.toString();
						lineBuffer.delete(0, lineBuffer.length());
						receivedMSP(line);
						continue;
					}

					// Search for indicator of lines beginning with "!!"
					if (text.equals("!")) {
						// Cache first ! if it is received
						if (lineBuffer.isEmpty()) {
							// Cache first !
							lineBuffer.append(print.getText());
							continue;
						} else if (lineBuffer.charAt(0)=='!' && lineBuffer.length()==1) {
							// Second ! - now we know we are receiving an MSP string
							lineBuffer.append(print.getText());
							inMSPMode=true;
							continue;
						}
					}
					lineBuffer.append(print.getText());
					if (inMSPMode)
						continue;

				}
				default -> {}
				}

				if (forwardTo!=null) {
					if (controlSequenceFilter!=null) {
						AParsedElement filtered = controlSequenceFilter.apply(frag);
						if (filtered!=null) {
							forwardTo.write(filtered);
							forwardTo.flush();
						} else
							logger.log(Level.WARNING, "Throw away "+frag);
					} else {
						forwardTo.write(frag);
//						if (config.isMissingGAWorkaround()) {
							forwardTo.flush(); // Usually we would flush upon receiving GA
//						}
					}
				} else {
					logger.log(Level.WARNING, "Read but ignored {0}={1}", frag.getName(), frag.toString());
				}

			}
		} catch (SocketException e) {
			logger.log(Level.ERROR, "Connection error: "+e.getMessage());
			return;
		} catch (Exception e) {
			logger.log(Level.ERROR, "Failed reading from MUD",e);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetOptionStatusChange(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetSubnegotiationHandler, boolean)
	 */
	@Override
	public void telnetOptionStatusChange(TelnetSocket nvt, TelnetOption option, boolean active) {
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetCommandReceived(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetCommand)
	 */
	public void telnetCommandReceived(TelnetSocket nvt, TelnetCommand command) {
		logger.log(Level.INFO, "Telnet Command received: "+command);
		try {
			switch (command.getCode()) {
			case GA:
			case EOR:
				if (inMXPMode) {
					logger.log(Level.WARNING, "Leave MXP due to EOR");
					inMXPMode=false;
					logger.log(Level.WARNING, "TODO: MXP: "+mxpBuffer);
				}
				if (forwardTo!=null) {
					forwardTo.flush();
				}
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void telnetSocketChanged(TelnetSocket nvt, State oldState, State newState) {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	private void receivedMXP(String line) {
		logger.log(Level.WARNING, "MXP: "+line);
	}

	//-------------------------------------------------------------------
	private void receivedMSP(String line) {
		logger.log(Level.WARNING, "MSP: "+line);
		lineBuffer.delete(0, lineBuffer.length());
		inMSPMode=false;

		MUDSoundProtocolListener listener = mud.getOptionListener(TelnetOption.MSP.getCode());
		try {
			if (listener!=null) {
				listener.mspReceivedCommand(line);
			} else
				logger.log(Level.WARNING, "No MSP listener");
		} catch (Exception e) {
			logger.log(Level.ERROR, "Failed interpreting MSP",e);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @param controlSequenceFilter the controlSequenceFilter to set
	 */
	public void setControlSequenceFilter(Function<AParsedElement, AParsedElement> controlSequenceFilter) {
		this.controlSequenceFilter = controlSequenceFilter;
	}

}
