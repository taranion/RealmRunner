package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.FilteringANSIStream;
import org.prelle.ansi.LinefeedToCRLFFilter;
import org.prelle.mxp.MXPInputStreamFilter;
import org.prelle.mxp.MXPOption;
import org.prelle.mxp.MXPOption.MXPListener;
import org.prelle.mxp.MxpSupportTable;
import org.prelle.realmrunner.feature.translate.LLMAutoTranslate;
import org.prelle.realmrunner.feature.tts.AutoTTS;
import org.prelle.telnet.CommunicationRole;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetListener;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetOptionListener;
import org.prelle.telnet.TelnetProtocol;
import org.prelle.telnet.mud.MUDSoundProtocolOption;
import org.prelle.realmrunner.feature.translate.LLMAutoTranslate;
import org.prelle.telnet.option.EchoMode;
import org.prelle.telnet.option.TelnetWindowSize;
import org.prelle.telnet.option.TerminalType;
import org.prelle.terminal.TerminalEmulator;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter
public class MUDSession implements TelnetListener, TelnetOptionListener {

	final static Logger logger = System.getLogger("mud.client");

	private String world;
	private TerminalEmulator console;
	private ANSIOutputStream streamToMUD;
	private FilteringANSIStream streamFromMUD;
	private TelnetProtocol telnet;
	
	private EchoMode echo;
	private MXPOption mxp;
	private LLMAutoTranslate translator;
	private AutoTTS tts;
	private MXPInputStreamFilter mxpFilter;
	@Setter
	private Consumer<MUDSession> sessionListener;

	//-------------------------------------------------------------------
	public static MUDSessionBuilder builder(TerminalEmulator terminal) {
		return new MUDSessionBuilder(terminal);
	}

	//-------------------------------------------------------------------
	public MUDSession(TerminalEmulator terminal, InputStream in, OutputStream out, Config config, MUDSessionBuilder builder) throws IOException {
		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
		this.console = terminal;
		this.telnet  = builder.telnet;
		this.world   = config.getServer().replaceAll("[^a-zA-Z0-9]", "_");
//		console.setLocalEchoActive(false);
//		console.setMode(TerminalMode.RAW);
		
		telnet.getInputStream().setSendGoAheadAsANSISepator(true);

//		console.getOutputStream().write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT520, true));
		
		streamToMUD   = new ANSIOutputStream(out);
//		streamFromMUD.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> input: {0}={1}", k, v));
//		streamToMUD.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> output: {0}={1}", k, v));
		
		streamFromMUD = terminal.connectWith(in, streamToMUD);
		
		setupECHO();
		setupNAWS();
		setupTTYPE(builder.terminalTypes);
		setupMSP();
		
		if (config.isMXPEnabled()) {
			setupMXP();
		}
		setupTranslator(config);
		
		// Is this a MUD that only sends LF, insteadt of CR LF
		if (config.getDoesNotSendCR()!=null && config.getDoesNotSendCR()) {
			streamFromMUD.addFilter(new LinefeedToCRLFFilter());
		}
		
		terminal.start();
	}
	
	//-------------------------------------------------------------------
	private void fireSessionChanged() {
		if (sessionListener!=null) sessionListener.accept(this);
	}
	
	//-------------------------------------------------------------------
	private void setupECHO() {
		logger.log(Level.INFO, "ENTER: setupECHO");
		echo = new EchoMode();
		telnet.add(echo);
	}

	//-------------------------------------------------------------------
	private void setupNAWS() {
		logger.log(Level.INFO, "ENTER: setupNAWS");
		var naws = new TelnetWindowSize();
		telnet.add(naws);
		console.addConsoleSizeListener( size -> {
			try {
				logger.log(Level.INFO, "Console size changed to {0}x{1}", size[0],size[1]);
				naws.update(telnet, size[0], size[1]);
			} catch (IOException e) {
				logger.log(Level.ERROR, "Failed sending NAWS update", e);
			}
		});
		logger.log(Level.INFO, "LEAVE: setupNAWS");
	}
	
	//-------------------------------------------------------------------
	private void setupTTYPE(String[] terminalTypes) {
		logger.log(Level.INFO, "ENTER: setupTTYPE");
		telnet.add(new TerminalType(terminalTypes!=null ? terminalTypes: new String[] {"xterm-256color"}));
	}
	
	//-------------------------------------------------------------------
	private void setupMXP() {
		logger.log(Level.INFO, "ENTER: setupMXP");
		mxp = new MXPOption(CommunicationRole.CLIENT,"b");
		mxp.addListener(new MXPListener() {
			@Override
			public void telnetMXPLearned(MxpSupportTable data) { }
			@Override
			public void mxpDTDChanged(String newDTD) {
				fireSessionChanged();
			}
			@Override
			public void mxpClientInfo(String client, String version, String mxpVersion, String style) {
				// TODO Auto-generated method stub
				
			}
		});
		telnet.add(mxp);
		
		mxpFilter = new MXPInputStreamFilter(mxp);
		telnet.addListener( (TelnetListener)mxpFilter);
		streamFromMUD.addFilter(mxpFilter);
	}
	
	//-------------------------------------------------------------------
	private void setupMSP() {
		telnet.add(new MUDSoundProtocolOption(CommunicationRole.CLIENT));
		console.getReadBuffer().addReadBufferHandler(new MSPHandler(this));
	}
	
	//-------------------------------------------------------------------
	private void setupTranslator(Config config) {	
		translator = new LLMAutoTranslate(this, Locale.ENGLISH);
		console.getReadBuffer().addReadBufferHandler(translator);
	}
	
	//-------------------------------------------------------------------
	private void setupTextToSpeech(Config config) {	
		tts = new AutoTTS(this);
		console.getReadBuffer().addReadBufferHandler(tts);
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
		try {
			streamToMUD.close();
//			streamFromMUD.close();
//			socket.close();
			getConsole().close();
			
			if (translator!=null) translator.onConnectionLost();
			if (tts!=null) tts.onConnectionLost();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void optionStateChanged(TelnetOption extension, boolean active) {
		// TODO Auto-generated method stub
		logger.log(Level.WARNING, "Option: {0}={1}", extension, active ? " activated" : " deactivated");
		if (extension==echo) {
			console.setLocalEchoActive(!active);
		}
	}

	@Override
	public void telnetCommandReceived(TelnetCommand command) {
		// TODO Auto-generated method stub
		logger.log(Level.WARNING, "RCV Telnet command: {0}", command);
		switch (command.getCode()) {
		case GA:
		case EOR:
			console.releaseInputBuffer();
			break;
		}
	}

//	//-------------------------------------------------------------------
//	static void configureTelnetProtocol(MUDSessionBuilder builder,TelnetProtocol protocol, Config config) {
//		logger.log(Level.INFO, "ENTER: configureTelnetProtocol");
//		
//		// Prepare NAWS
//		var naws = new TelnetWindowSize();
//		builder.terminal.addConsoleSizeListener( size -> {
//			try {
//				logger.log(Level.INFO, "Console size changed to {0}x{1}", size[0],size[1]);
//				naws.update(protocol, size[0], size[1]);
//			} catch (IOException e) {
//				logger.log(Level.ERROR, "Failed sending NAWS update", e);
//			}
//		});
//		
//		protocol.add(new TerminalType(builder.terminalTypes!=null ? builder.terminalTypes : new String[] {"xterm-256color"}))
//				.add(naws)
//				;
//		
//		// Prepare MXP
//		if (config.isMXPEnabled()) {
//			var mxp = new MXPOption(CommunicationRole.CLIENT,"b");
//			protocol.add(mxp);
//			
//		}
//		logger.log(Level.INFO, "LEAVE: configureTelnetProtocol");
//	}
	
	//-------------------------------------------------------------------
	/**
	 * Return custom MXP tags defined by the MUD server.
	 */
	public Optional<String> getMXPDefinitions() {
		if (mxpFilter!=null) {
			return Optional.ofNullable(mxpFilter.getDTD());
		}
		return Optional.empty();
	}

	@Override
	public void telnetReady() {
		// TODO Auto-generated method stub
		
	}

}
