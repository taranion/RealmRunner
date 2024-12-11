package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Code;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetConstants;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.option.LineMode.LineModeListener;
import org.prelle.telnet.option.LineMode.ModeBit;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

/**
 *
 */
public class ReadFromConsoleTask implements Runnable, LineModeListener {

	private final static Logger logger = System.getLogger("mud.client");

	private TerminalEmulator console;
	private ANSIOutputStream forwardTo;
	private AbstractConfig config;
	private StringBuffer lineBuffer;
	private List<Integer> flushOnThisCodes = new ArrayList<>(List.of(10,13));
	private boolean forwardMode = true;
	private Consumer<AParsedElement> whenNotForwarding;

	private boolean lineBufferingMode;
	private boolean mustCreateLocalEcho;
	private LineBufferListener lineBufferListener;

	//-------------------------------------------------------------------
	public ReadFromConsoleTask(TerminalEmulator console, AbstractConfig config, LineBufferListener listener) throws IOException {
		this.console = console;
		this.config  = config;
		this.lineBufferListener = listener;
		lineBuffer   = new StringBuffer();
		lineBufferingMode = true;
		updateMode();
		console.getInputStream().setCollectPrintable(false);
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		ANSIInputStream in = console.getInputStream();
		while (true) {
			try {
				AParsedElement fragment = in.readFragment();
				logger.log(Level.TRACE, "Typed {0}  forwardMode={1}, mustLocalEcho={2}, lineBuffering={3}", fragment, forwardMode, mustCreateLocalEcho, lineBufferingMode);

				switch (fragment) {
				case PrintableFragment print -> {
					if (lineBufferingMode) {
						logger.log(Level.DEBUG, "Add to linebuffer: "+print.getText());
						lineBuffer.append(print.getText());
						lineBufferListener.lineBufferChanged(lineBuffer.toString(), lineBuffer.length());
						continue;
					}}
				case C0Fragment c0 -> {
					switch (c0.getCode()) {
					case C0Code.LF:
					case C0Code.CR:
						if (lineBufferingMode && !lineBuffer.isEmpty()) {
							logger.log(Level.INFO, "ENTER");
							console.getOutputStream().write("\r\n");
							String toSend = lineBufferListener.processCommandTyped(lineBuffer.toString());
							if (toSend!=null && forwardTo!=null) {
								forwardTo.write(toSend+"\r\n");
							}
							lineBuffer.delete(0, lineBuffer.length());
							lineBufferListener.lineBufferChanged("", 0);
							continue;
						} else
							console.getOutputStream().write("\r\n");
						break;
					case C0Code.DEL:
						if (lineBufferingMode && !lineBuffer.isEmpty()) {
							logger.log(Level.DEBUG, "Delete last char from linebuffer");
							lineBuffer.deleteCharAt(lineBuffer.length()-1);
							lineBufferListener.lineBufferChanged(lineBuffer.toString(), lineBuffer.length());
							continue;
						} else
							logger.log(Level.INFO, "HUHU");
						break;
					}
				}
				default -> { }
				}
				logger.log(Level.DEBUG, "fall through - forward = "+forwardMode+"  whenNot="+whenNotForwarding);


				if (mustCreateLocalEcho) {
//					logger.log(Level.WARNING, "  echo");
					console.getOutputStream().write(fragment);
				}
				if (!forwardMode) {
					if (whenNotForwarding!=null)
						whenNotForwarding.accept(fragment);
					continue;
				}

				if (forwardTo!=null) {
					logger.log(Level.DEBUG, "Forward console input to "+forwardTo);
					// Always send text and C0 fragments
					// everything else only when not linebuffering
					switch (fragment) {
					case PrintableFragment print -> {
//						if (isLineBuffering) {
//							logger.log(Level.INFO, "Flush {0}", lineBuffer.toString());
//							lineBuffer.append( print.getText() );
//						} else {
							forwardTo.write(print);
//						}
						forwardTo.flush();
					}
					case C0Fragment c0 -> {
						int code = c0.getCode().code();
						// Check if linemode requires that we flush
						if (flushOnThisCodes.contains(code)) {
							logger.log(Level.INFO, "Flush {0}", lineBuffer.toString());
							if (!lineBuffer.isEmpty()) {
								forwardTo.write(lineBuffer.toString());
								lineBuffer.delete(0, lineBuffer.length());
							}
							forwardTo.flush();
						}
						// Check if we want to enforce a CR sent before every LF
						switch (c0.getCode()) {
						case C0Code.LF:
							//if (config.isSendCRbeforeLF()) {
							forwardTo.write(C0Code.LF);
							logger.log(Level.INFO, "Inject CR");
							forwardTo.write(C0Code.CR);
							//}
							break;
						case C0Code.CR:
							logger.log(Level.INFO, "Don't Swallow CR");
							forwardTo.write(C0Code.CR);
							break;
						default:
							logger.log(Level.DEBUG, "Forward "+c0);
							forwardTo.write(c0);
						}
					}
					default -> {
						logger.log(Level.WARNING, "TODO: Check for cursor movement");
						if (config.getIgnoreControlCodesFromTerminal()==null || !config.getIgnoreControlCodesFromTerminal()) {
//							if (!isLineBuffering) {
							logger.log(Level.WARNING, "  send to MUD "+fragment);
							forwardTo.write(fragment);
							forwardTo.flush();
						} else {
							logger.log(Level.INFO, "Swallow {0}", fragment);
						}
					  }
					} // switch
				} // if (forwardTo!=null)
			} catch (SocketException e) {
				logger.log(Level.ERROR, "Connection lost");
				try { 
					console.getOutputStream().write("\r\nConnection lost.r\n"); 
					forwardTo.close();
				} catch (IOException ee) {}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//-------------------------------------------------------------------
	private void updateMode() {
		boolean isLineBuffering = console.getMode()==TerminalMode.LINE_MODE;
		if (isLineBuffering) {
			logger.log(Level.INFO, "Linebuffer active - ignore cursor keys and control sequences");
			console.getInputStream().setCollectPrintable(true);
		} else {
			logger.log(Level.INFO, "Raw mode active - send cursor keys and control sequences");
			console.getInputStream().setCollectPrintable(false);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.option.LineMode.LineModeListener#linemodeFlagsSuggested(java.util.List)
	 */
	@Override
	public List<ModeBit> linemodeFlagsSuggested(List<ModeBit> suggested) {
		logger.log(Level.INFO, "Flags suggested: {0}", suggested);
		boolean lineMode = suggested.contains(ModeBit.EDIT);
		console.setLocalEchoActive(lineMode);
		console.setMode(lineMode?TerminalMode.LINE_MODE:TerminalMode.RAW);
		updateMode();
		return suggested;
	}

	//-------------------------------------------------------------------
	@Override
	public void linemodeFlagsAcknowledged(List<ModeBit> acknowledged) {
		acknowledged.remove(ModeBit.MODE_ACK);
		logger.log(Level.INFO, "linemodeFlagsAcknowledged {0} ",acknowledged);
		
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.option.LineMode.LineModeListener#sendFlushOn(java.util.List)
	 */
	@Override
	public void sendFlushOn(List<Integer> flushCodes) {
		logger.log(Level.INFO, "Flush buffer on: {0}", flushCodes);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetOptionListener#remotePartySent(org.prelle.telnet.TelnetSocket, int, org.prelle.telnet.TelnetCommand)
	 */
	@Override
	public void remotePartySent(TelnetSocket socket, int code, TelnetCommand command) {
		try {
			switch (code) {
			case 1: // ECHO
				switch (command.getCode().code()) {
				case TelnetConstants.WILL:
					logger.log(Level.INFO, "disable local echo creation");
					mustCreateLocalEcho=false;
					socket.out().sendDo(code);
					break;
				case TelnetConstants.WONT:
					logger.log(Level.INFO, "enable local echo creation");
					mustCreateLocalEcho=true;
					socket.out().sendDont(code);
					break;
				}
				break;
			default:
				logger.log(Level.INFO, "TODO: remotePartySent {0}", command);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	public void setForwardMode(boolean value) {
		logger.log(Level.INFO, "Set forward mode to {0}", value);
		this.forwardMode = value;
		updateMode();
	}

	//-------------------------------------------------------------------
	public void setForwardTo(TelnetSocket mud) throws IOException {
		this.forwardTo = new ANSIOutputStream(mud.getOutputStream());
		forwardTo.setLoggingListener( (type,text) -> logger.log(Level.INFO, "MUD --> {0} = {1}", type,text));
		mud.setOptionListener(TelnetOption.LINEMODE, (LineModeListener)this);
		mud.setOptionListener(TelnetOption.ECHO, this);
	}

	//-------------------------------------------------------------------
	/**
	 * @param whenNotForwarding the whenNotForwarding to set
	 */
	public void setWhenNotForwarding(Consumer<AParsedElement> whenNotForwarding) {
		this.whenNotForwarding = whenNotForwarding;
	}

}
