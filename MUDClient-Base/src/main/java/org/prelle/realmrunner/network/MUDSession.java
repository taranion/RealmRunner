package org.prelle.realmrunner.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.DeviceAttributes.OperatingLevel;
import org.prelle.ansi.commands.SetConformanceLevel;
import org.prelle.mud4j.gmcp.GMCPManager;
import org.prelle.mud4j.gmcp.Char.Stats;
import org.prelle.mud4j.gmcp.Char.Vitals;
import org.prelle.mud4j.gmcp.Client.ClientMediaPackage;
import org.prelle.mud4j.gmcp.Client.ClientMediaPlay;
import org.prelle.mud4j.gmcp.Client.ClientMediaStop;
import org.prelle.mud4j.gmcp.Room.GMCPRoomInfo;
import org.prelle.mud4j.gmcp.beip.BeipTilemapData;
import org.prelle.mud4j.gmcp.beip.BeipTilemapInfo;
import org.prelle.mudansi.CapabilityDetector;
import org.prelle.mudansi.TerminalCapabilities;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetConstants.ControlCode;
import org.prelle.telnet.TelnetInputStream;
import org.prelle.telnet.TelnetListener;
import org.prelle.telnet.TelnetOptionListener;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSubnegotiationHandler;
import org.prelle.telnet.WellKnownTelnetOptions;
import org.prelle.telnet.mud.AardwolfMushclientProtocol;
import org.prelle.telnet.mud.GenericMUDCommunicationProtocol.GMCPReceiver;
import org.prelle.telnet.mud.GenericMUDCommunicationProtocol.RawGMCPMessage;
import org.prelle.telnet.mud.MUDTerminalTypeData;
import org.prelle.telnet.option.TelnetEnvironmentOption;
import org.prelle.telnet.option.TelnetWindowSize;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import lombok.Getter;

/**
 *
 */
@Getter
public class MUDSession implements TelnetListener, TelnetOptionListener, GMCPReceiver {

	private final static Logger logger = System.getLogger("mud.client");

	public static class Builder {

		private TerminalEmulator terminal;
		private Config clientConfig;
		private SessionConfig sessionData;
		private TelnetListener telnetListener;
		private Charset charset;

		public Builder(TerminalEmulator terminal) {
			this.terminal = terminal;
		}
		public MUDSession build() throws Exception {
			ReadFromConsoleTask readFromConsole = new ReadFromConsoleTask(terminal, clientConfig, (LineBufferListener)null);

			Thread readFromTerminal = new Thread(readFromConsole, "FromConsole");
			readFromTerminal.start();

			terminal.getOutputStream().write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT520, true));
			readFromConsole.setForwardMode(false);

			MUDSession session = new MUDSession(sessionData, telnetListener, terminal.getConsoleSize(), charset);
			return session;
		}
		//-------------------------------------------------------------------
		public Builder setCharset(Charset value) { this.charset = value; return this; }
	}

	private TerminalEmulator console;
	private Charset charset;
	private ReadFromConsoleTask readFromConsole;
	private TerminalCapabilities capabilities;

	private TelnetSocket socket;
	private ANSIOutputStream streamToMUD;
	private TelnetInputStream streamFromMUD;
	private Thread thread;

	private boolean characterMode = false;

	private TelnetWindowSize optNAWS;
	private MUDSessionGMCPListener gmcpListener;

	//-------------------------------------------------------------------
	public static Builder builder(TerminalEmulator terminal) {
		return new Builder(terminal);
	}

	//-------------------------------------------------------------------
	public MUDSession(TerminalEmulator terminal, TelnetListener callback, int[] naws, Charset charset) throws IOException {
		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
		this.console = terminal;
		console.setLocalEchoActive(false);
		console.setMode(TerminalMode.RAW);

		console.getOutputStream().write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT520, true));
		readFromConsole.setForwardMode(false);
		this.capabilities = new TerminalCapabilities();
		learnTerminal(readFromConsole);
	}

	//-------------------------------------------------------------------
	private void learnTerminal(ReadFromConsoleTask readTask) {
		logger.log(Level.DEBUG, "ENTER: learnTerminal");
		Charset[] encodings = console.getEncodings();
		logger.log(Level.INFO, "Encoding: Input={0}  Output={1}", encodings[0], encodings[1]);
		this.charset = encodings[1];


		ANSIOutputStream out = console.getOutputStream();
		CapabilityDetector detector = new CapabilityDetector(out);
		readTask.setWhenNotForwarding( frag -> {
			try {
				detector.process(frag);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		try {
			int[] size = console.getConsoleSize();
			capabilities = detector.performCheck(size[0], size[1]);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			capabilities.report(new ANSIOutputStream(baos));
			logger.log(Level.INFO, baos.toString(StandardCharsets.UTF_8));
			capabilities.report(new ANSIOutputStream(System.out));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.log(Level.DEBUG, "LEAVE: learnTerminal");

	}

	//-------------------------------------------------------------------
	@Deprecated
	public MUDSession(SessionConfig session, TelnetListener callback, int[] naws, Charset charset) throws IOException {
		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
//		TelnetOptionRegistry.register(WellKnownTelnetOptions.MUSHCLIENT.getCode(), new AardwolfMushclientProtocol());

		GMCPManager.registerPackage(new ClientMediaPackage());
//		GMCPManager.registerPackage(new CharPackage());
//		GMCPManager.registerPackage(new CharSkillsPackage());

		// Detect terminal type
		String term = System.getenv("TERM");
		if (term==null) term="xterm";
		// Detect environment data
		Map<String,String> environment = detectEnvironment();
		MUDTerminalTypeData mttData = new MUDTerminalTypeData()
				.setClientName("RealmRunner")
				.setTerminalType(term)
				;

		logger.log(Level.INFO, "Connecting to {0} port {1}", session.getServer(), session.getPort());
		socket = new TelnetSocket(session.getServer(), session.getPort())
				.addListener(callback)
				.setOptionListener(WellKnownTelnetOptions.ECHO, this)
////				.addSocketListener(new GMCPHandler(true))
//				.support(WellKnownTelnetOptions.ECHO.getCode(), ControlCode.WILL)
//				.support(WellKnownTelnetOptions.SGA.getCode(), ControlCode.DO)
//				.support(WellKnownTelnetOptions.EOR.getCode(), ControlCode.DO)
//				.support(WellKnownTelnetOptions.NEW_ENVIRON.getCode(), ControlCode.WILL, environment)
//				.support(WellKnownTelnetOptions.NAWS.getCode(), ControlCode.WILL, naws)
//				.support(WellKnownTelnetOptions.LINEMODE.getCode(), ControlCode.WILL)
//				.support(WellKnownTelnetOptions.TERMINAL_TYPE.getCode(), ControlCode.WILL, mttData)
//				.support(WellKnownTelnetOptions.MSP.getCode(), ControlCode.DO)
//				.support(WellKnownTelnetOptions.MXP.getCode(), ControlCode.DO)
//				.support(WellKnownTelnetOptions.GMCP.getCode(), ControlCode.DO)
//				.support(WellKnownTelnetOptions.MUSHCLIENT.getCode(), ControlCode.DO)
//				.support(WellKnownTelnetOptions.CHARSET.getCode(), ControlCode.DO, charset)
////				.support(new MUDClientCompression1(), Role.REJECT_OUTRIGHT)
////				.support(new MUDClientCompression2(), Role.REJECT_OUTRIGHT)
////				.support(new ZenithMUDProtocol(), Role.REJECT_OUTRIGHT)
				;
		socket.setTcpNoDelay(true);
		socket.getStack().add(new TelnetEnvironmentOption(environment, null));
		logger.log(Level.INFO, "Register MUDSession as GMCP listener");
//		socket.setOptionListener(WellKnownTelnetOptions.GMCP, this);
		streamToMUD   = new ANSIOutputStream( socket.getOutputStream());
		streamFromMUD = (TelnetInputStream) socket.getInputStream();
		logger.log(Level.INFO, "LEAVE: MUDSession.<init>");
	}

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

	//-------------------------------------------------------------------
	public void sendWindowSizeUpdate(int width, int height) throws IOException {
		TelnetWindowSize.sendUpdate(socket, width, height);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.mud.GenericMUDCommunicationProtocol.GMCPReceiver#telnetReceiveGMCP(org.prelle.telnet.mud.GenericMUDCommunicationProtocol.RawGMCPMessage)
	 */
	@Override
	public void telnetReceiveGMCP(RawGMCPMessage gmcp) {
		logger.log(Level.DEBUG, "GMCP RCV "+gmcp.getNamespace()+"  "+gmcp.getMessage());
		Object mess = GMCPManager.decode(gmcp.getNamespace(), gmcp.getMessage());
		if (mess==null) {
			logger.log(Level.WARNING, "No parsing support for {0} {1}", gmcp.getNamespace(), gmcp.getMessage());
			return;
		}
		if (gmcpListener==null) {
			logger.log(Level.WARNING, "No handler for GMCP "+mess);
			return;
		}

		switch (mess) {
		case BeipTilemapInfo info -> gmcpListener.gmcpBeipTilemapInfo(info);
		case BeipTilemapData data -> gmcpListener.gmcpBeipTilemapUpdate(data);
		case ClientMediaPlay play -> gmcpListener.gmcpReceivedClientMedia(play);
		case ClientMediaStop stop -> gmcpListener.gmcpReceivedClientMedia(stop);
		case GMCPRoomInfo room -> gmcpListener.gmcpReceivedRoomInfo(room);
		case Stats stats -> gmcpListener.gmcpReceivedStats(stats);
		case Vitals vitals -> gmcpListener.gmcpReceivedVitals(vitals);
		case String strMess when gmcp.getNamespace().equals("Core.Goodbye") -> {
			logger.log(Level.WARNING, "Server closed connection with message ''{0}''", strMess);
			close();
		}
		default -> {
			logger.log(Level.WARNING, "Don't know what to do for "+mess);
		}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @param gmcpListener the gmcpListener to set
	 */
	public void setGmcpListener(MUDSessionGMCPListener gmcpListener) {
		this.gmcpListener = gmcpListener;
	}

	//-------------------------------------------------------------------
	public void close() {
		logger.log(Level.WARNING, "closing session");
		try {
			streamToMUD.close();
			streamFromMUD.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void optionStateChanged(TelnetSubnegotiationHandler extension, boolean active) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void telnetCommandReceived(TelnetCommand command) {
		// TODO Auto-generated method stub
		
	}

}
