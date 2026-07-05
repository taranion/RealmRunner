package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.DeviceAttributes.OperatingLevel;
import org.prelle.ansi.FilteringANSIStream;
import org.prelle.ansi.commands.SetConformanceLevel;
import org.prelle.telnet.CommunicationRole;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetConstants.ControlCode;
import org.prelle.telnet.TelnetInputStream;
import org.prelle.telnet.TelnetListener;
import org.prelle.telnet.TelnetOptionListener;
import org.prelle.telnet.TelnetOutputStream;
import org.prelle.telnet.TelnetProtocol;
import org.prelle.telnet.TelnetSubnegotiationHandler;
import org.prelle.telnet.option.MXPOption;
import org.prelle.telnet.option.TelnetWindowSize;
import org.prelle.telnet.option.TerminalType;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import lombok.Getter;

/**
 *
 */
@Getter
public class MUDSession implements TelnetListener, TelnetOptionListener {

	private final static Logger logger = System.getLogger("mud.client");

	public static class Builder {

		private TerminalEmulator terminal;
		private Config clientConfig;
		private SessionConfig sessionData;
		private Charset charset;
		private String[] terminalTypes;
		

		public Builder(TerminalEmulator terminal) {
			this.terminal = terminal;
		}
		public MUDSession build() throws IOException {
			try {
				InetAddress host = InetAddress.getByName(clientConfig.getServer());
				
				MUDConnection con = switch (clientConfig.getProtocol()) {
					case TELNET    -> new TCPMUDConnection(host, clientConfig.getPort());
					case WEBSOCKET -> new WebsocketMUDConnection(host, clientConfig.getPort());
					default -> throw new IllegalArgumentException("Unsupported protocol "+clientConfig.getProtocol());
				};
				logger.log(Level.DEBUG, "New connection: {0}", con);
				InputStream in = con.getStreamFromMUD();
				OutputStream out = con.getStreamToMUD();
				if (con instanceof TCPMUDConnection || (con instanceof WebsocketMUDConnection ws && ws.getNegotiatedSubprotocol().contains("telnet")) ) {
					logger.log(Level.WARNING, "TODO: Initialize telnet wrapper");			
					try {
						TelnetProtocol protocol = new TelnetProtocol(CommunicationRole.CLIENT);
						MUDSession.configureTelnetProtocol(this, protocol, clientConfig);
						protocol.addListener(new TelnetListener() {
							@Override
							public void telnetCommandReceived(TelnetCommand command) {
								logger.log(Level.WARNING, "Telnet command received: {0}", command);
							}
							
							@Override
							public void optionStateChanged(TelnetSubnegotiationHandler extension, boolean active) {
								logger.log(Level.WARNING, "Telnet option state changed: {0} active={1}", extension, active);
							}
						});
						out = new TelnetOutputStream(out, protocol);
						protocol.setOutputStream((TelnetOutputStream) out);
						in = new TelnetInputStream(in, protocol);
						protocol.setInputStream( (TelnetInputStream) in);
						((TelnetInputStream)in).setReverseStream((TelnetOutputStream) out);
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				MUDSession session = new MUDSession(terminal, in, out, clientConfig);
				if (in instanceof TelnetInputStream tin) {
					tin.getProtocol().addListener(session);
				}
				return session;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(0);
			return null;
		}
		//-------------------------------------------------------------------
		public Builder setCharset(Charset value) { this.charset = value; return this; }
		//-------------------------------------------------------------------
		public Builder setConfig(SessionConfig value) { this.sessionData = value; return this; }
		//-------------------------------------------------------------------
		public Builder setClientConfig(Config value) { this.clientConfig = value; return this; }
		//-------------------------------------------------------------------
		public Builder setTerminalTypes(String...value) { this.terminalTypes = value; return this; }
	}

	private TerminalEmulator console;
//	private Charset charset;
//	private ReadFromConsoleTask readFromConsole;
//	private TerminalCapabilities capabilities;
//
//	private TelnetSocket socket;
	private ANSIOutputStream streamToMUD;
	private FilteringANSIStream streamFromMUD;
//	private Thread thread;
//
//	private boolean characterMode = false;
//
//	private TelnetWindowSize optNAWS;
//	private MUDSessionGMCPListener gmcpListener;

	//-------------------------------------------------------------------
	public static Builder builder(TerminalEmulator terminal) {
		return new Builder(terminal);
	}

	//-------------------------------------------------------------------
	public MUDSession(TerminalEmulator terminal, InputStream in, OutputStream out, Config config) throws IOException {
		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
		this.console = terminal;
		console.setLocalEchoActive(false);
		console.setMode(TerminalMode.RAW);

//		console.getOutputStream().write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT520, true));
		
		streamToMUD   = new ANSIOutputStream(out);
//		streamFromMUD.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> input: {0}={1}", k, v));
//		streamToMUD.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> output: {0}={1}", k, v));
		
		
		streamFromMUD = terminal.connectWith(in, streamToMUD);
		
		
//		terminal.connectWith(con.getStreamFromMUD(), con.getStreamToMUD());
//		readFromConsole.setForwardMode(false);
//		this.capabilities = new TerminalCapabilities();
//		learnTerminal(readFromConsole);
	}

//	//-------------------------------------------------------------------
//	private void learnTerminal(ReadFromConsoleTask readTask) {
//		logger.log(Level.DEBUG, "ENTER: learnTerminal");
//		Charset[] encodings = console.getEncodings();
//		logger.log(Level.INFO, "Encoding: Input={0}  Output={1}", encodings[0], encodings[1]);
//		this.charset = encodings[1];
//
//
//		ANSIOutputStream out = console.getOutputStream();
//		CapabilityDetector detector = new CapabilityDetector(out);
//		readTask.setWhenNotForwarding( frag -> {
//			try {
//				detector.process(frag);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
//		try {
//			int[] size = console.getConsoleSize();
//			capabilities = detector.performCheck(size[0], size[1]);
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			capabilities.report(new ANSIOutputStream(baos));
//			logger.log(Level.INFO, baos.toString(StandardCharsets.UTF_8));
//			capabilities.report(new ANSIOutputStream(System.out));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		logger.log(Level.DEBUG, "LEAVE: learnTerminal");
//
//	}
//
//	//-------------------------------------------------------------------
//	@Deprecated
//	public MUDSession(SessionConfig session, int[] naws, Charset charset) throws IOException {
//		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
////		TelnetOptionRegistry.register(WellKnownTelnetOptions.MUSHCLIENT.getCode(), new AardwolfMushclientProtocol());
//
//		GMCPManager.registerPackage(new ClientMediaPackage());
////		GMCPManager.registerPackage(new CharPackage());
////		GMCPManager.registerPackage(new CharSkillsPackage());
//
//		// Detect terminal type
//		String term = System.getenv("TERM");
//		if (term==null) term="xterm";
//		// Detect environment data
//		Map<String,String> environment = detectEnvironment();
//		MUDTerminalTypeData mttData = new MUDTerminalTypeData()
//				.setClientName("RealmRunner")
//				.setTerminalType(term)
//				;
//
//		logger.log(Level.INFO, "Connecting to {0} port {1}", session.getServer(), session.getPort());
//		socket = new TelnetSocket(session.getServer(), session.getPort())
//				.addListener(this)
//				.setOptionListener(WellKnownTelnetOptions.ECHO, this)
//////				.addSocketListener(new GMCPHandler(true))
////				.support(WellKnownTelnetOptions.ECHO.getCode(), ControlCode.WILL)
////				.support(WellKnownTelnetOptions.SGA.getCode(), ControlCode.DO)
////				.support(WellKnownTelnetOptions.EOR.getCode(), ControlCode.DO)
////				.support(WellKnownTelnetOptions.NEW_ENVIRON.getCode(), ControlCode.WILL, environment)
////				.support(WellKnownTelnetOptions.NAWS.getCode(), ControlCode.WILL, naws)
////				.support(WellKnownTelnetOptions.LINEMODE.getCode(), ControlCode.WILL)
////				.support(WellKnownTelnetOptions.TERMINAL_TYPE.getCode(), ControlCode.WILL, mttData)
////				.support(WellKnownTelnetOptions.MSP.getCode(), ControlCode.DO)
////				.support(WellKnownTelnetOptions.MXP.getCode(), ControlCode.DO)
////				.support(WellKnownTelnetOptions.GMCP.getCode(), ControlCode.DO)
////				.support(WellKnownTelnetOptions.MUSHCLIENT.getCode(), ControlCode.DO)
////				.support(WellKnownTelnetOptions.CHARSET.getCode(), ControlCode.DO, charset)
//////				.support(new MUDClientCompression1(), Role.REJECT_OUTRIGHT)
//////				.support(new MUDClientCompression2(), Role.REJECT_OUTRIGHT)
//////				.support(new ZenithMUDProtocol(), Role.REJECT_OUTRIGHT)
//				;
//		socket.setTcpNoDelay(true);
//		socket.getStack().add(new TelnetEnvironmentOption(environment, null));
//		logger.log(Level.INFO, "Register MUDSession as GMCP listener");
////		socket.setOptionListener(WellKnownTelnetOptions.GMCP, this);
//		streamToMUD   = new ANSIOutputStream( socket.getOutputStream());
//		streamFromMUD = (TelnetInputStream) socket.getInputStream();
//		logger.log(Level.INFO, "LEAVE: MUDSession.<init>");
//	}

	//-------------------------------------------------------------------
	/**
	 * @return
	 */
	private Map<String, String> detectEnvironment() {
		Map<String,String> ret = new HashMap<>();
		for (Entry<String, String> entry : System.getenv().entrySet()) {
			switch (entry.getKey()) {
			case "COLORTERM":
			case "HOME":
			case "HOSTNAME":
			case "HOSTTYPE":
			case "KITTY_PUBLIC_KEY":
			case "LANG":
			case "TERM":
			case "USER":
			case "USERNAME":
				ret.put(entry.getKey(), entry.getValue());
				break;
			}
		}
		return ret;
	}

//	//-------------------------------------------------------------------
//	public void sendWindowSizeUpdate(int width, int height) throws IOException {
//		TelnetWindowSize.sendUpdate(socket, width, height);
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.telnet.mud.GenericMUDCommunicationProtocol.GMCPReceiver#telnetReceiveGMCP(org.prelle.telnet.mud.GenericMUDCommunicationProtocol.RawGMCPMessage)
//	 */
//	@Override
//	public void telnetReceiveGMCP(RawGMCPMessage gmcp) {
//		logger.log(Level.DEBUG, "GMCP RCV "+gmcp.getNamespace()+"  "+gmcp.getMessage());
//		Object mess = GMCPManager.decode(gmcp.getNamespace(), gmcp.getMessage());
//		if (mess==null) {
//			logger.log(Level.WARNING, "No parsing support for {0} {1}", gmcp.getNamespace(), gmcp.getMessage());
//			return;
//		}
//		if (gmcpListener==null) {
//			logger.log(Level.WARNING, "No handler for GMCP "+mess);
//			return;
//		}
//
//		switch (mess) {
//		case BeipTilemapInfo info -> gmcpListener.gmcpBeipTilemapInfo(info);
//		case BeipTilemapData data -> gmcpListener.gmcpBeipTilemapUpdate(data);
//		case ClientMediaPlay play -> gmcpListener.gmcpReceivedClientMedia(play);
//		case ClientMediaStop stop -> gmcpListener.gmcpReceivedClientMedia(stop);
//		case GMCPRoomInfo room -> gmcpListener.gmcpReceivedRoomInfo(room);
//		case Stats stats -> gmcpListener.gmcpReceivedStats(stats);
//		case Vitals vitals -> gmcpListener.gmcpReceivedVitals(vitals);
//		case String strMess when gmcp.getNamespace().equals("Core.Goodbye") -> {
//			logger.log(Level.WARNING, "Server closed connection with message ''{0}''", strMess);
//			close();
//		}
//		default -> {
//			logger.log(Level.WARNING, "Don't know what to do for "+mess);
//		}
//		}
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @param gmcpListener the gmcpListener to set
//	 */
//	public void setGmcpListener(MUDSessionGMCPListener gmcpListener) {
//		this.gmcpListener = gmcpListener;
//	}

	//-------------------------------------------------------------------
	public void close() {
		logger.log(Level.WARNING, "closing session");
//		try {
//			streamToMUD.close();
//			streamFromMUD.close();
//			socket.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@Override
	public void optionStateChanged(TelnetSubnegotiationHandler extension, boolean active) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void telnetCommandReceived(TelnetCommand command) {
		// TODO Auto-generated method stub
		logger.log(Level.WARNING, "RCV Telnet command: {0}", command);
	}

	private static void configureTelnetProtocol(MUDSession.Builder builder,TelnetProtocol protocol, Config config) {
		logger.log(Level.INFO, "ENTER: configureTelnetProtocol");
		
		// Prepare NAWS
		var naws = new TelnetWindowSize();
		builder.terminal.addConsoleSizeListener( size -> {
			try {
				logger.log(Level.INFO, "Console size changed to {0}x{1}", size[0],size[1]);
				naws.update(protocol, size[0], size[1]);
			} catch (IOException e) {
				logger.log(Level.ERROR, "Failed sending NAWS update", e);
			}
		});
		
		protocol.add(new TerminalType(builder.terminalTypes!=null ? builder.terminalTypes : new String[] {"xterm-256color"}))
				.add(naws)
				;
		
		// Prepare MXP
		if (config.isMXPEnabled()) {
			var mxp = new MXPOption(CommunicationRole.CLIENT,"b");
			protocol.add(mxp);
			
		}
		logger.log(Level.INFO, "LEAVE: configureTelnetProtocol");
	}
}
