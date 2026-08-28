package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.prelle.mud4j.gmcp.GMCPHandler;
import org.prelle.mudevents.MUDEventPipeline;
import org.prelle.mudevents.MUDEventProcessor;
import org.prelle.mudevents.PipeEvent;
import org.prelle.mudevents.StartEvent;
import org.prelle.mudevents.ansi.ANSILayer;
import org.prelle.mudevents.telnet.MUDClientTelnet;
import org.prelle.mxp.stream.MXPInputStreamFilter;
import org.prelle.mxp.telnet.MXPOption;
import org.prelle.realmrunner.feature.translate.LLMAutoTranslate;
import org.prelle.realmrunner.feature.tts.AutoTTS;
import org.prelle.telnet.mud.MUDServerStatusProtocol;
import org.prelle.telnet.option.CommunicationRole;
import org.prelle.telnet.option.EchoMode;
import org.prelle.telnet.option.TelnetWindowSize;
import org.prelle.terminal.InputBuffer;
import org.prelle.terminal.MessageLog;
import org.prelle.terminal.TerminalEmulator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter
public class MUDSession implements MUDEventProcessor {

	final static Logger logger = System.getLogger("mud.client");

	private String subprotocol;
	/** TRUE, if connection has been closed */
	private boolean closed;
	private String world;
	private MUDSessionUserInterface ui;
	private TerminalEmulator console;
	
	private MUDConnection connection;
	
	private MUDEventPipeline streamToMUD;
	private MUDEventPipeline streamFromMUD;
	
	private EchoMode echo;
	private MXPOption mxp;
	private GMCPHandler gmcp;
	private LLMAutoTranslate translator;
	private AutoTTS tts;
	private MXPInputStreamFilter mxpFilter;
	@Setter
	private Consumer<MUDSession> sessionListener;
    
    private MessageLog messageLog = new MessageLog();

//	//-------------------------------------------------------------------
//	public static MUDSessionBuilder builder(TerminalEmulator terminal) {
//		return new MUDSessionBuilder(terminal);
//	}

	//-------------------------------------------------------------------
    @Builder(setterPrefix = "with")
    public MUDSession(String world, URI target, Charset encoding, MUDSessionUserInterface userInterface, String websocketSubprotocol) throws IOException {
		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
		Objects.requireNonNull(userInterface, "User interface must be set ");
		Objects.requireNonNull(world, "World name must be set");
		this.ui = userInterface;
		this.world = world;
		this.subprotocol = websocketSubprotocol;
		
		streamToMUD   = new MUDEventPipeline("SND").then(new InputBuffer());
		
		connection = createConnection(target);
		streamFromMUD = connection.getReceivePipe();
		
		if (connection.isSupportsTelnet()) {
			configureClassicTelnet(connection);
		} else if (connection.isSupportsMUDDown()) {
			configureMUDDown(connection);
		}
		
		withUserInterface(userInterface);
	}
    
	//-------------------------------------------------------------------
    private MUDConnection createConnection(URI target) throws IOException {
    	InetAddress host = InetAddress.getByName(target.getHost());
		String scheme = target.getScheme();
		if (scheme == null || scheme.equals("telnet")) {
			TCPMUDConnection var= new TCPMUDConnection(host, target.getPort() != -1 ? target.getPort() : 23);
			return var;
		} else if (scheme == null || scheme.equals("telnets")) {
			TCPSSLMUDConnection var = new TCPSSLMUDConnection(host, target.getPort() != -1 ? target.getPort() : 992);
			return var;
		} else if (scheme.equals("ws") || scheme.equals("wss")) {
			return new WebsocketMUDConnection(target, subprotocol);
		} else {
			throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
		}
	}
    
	//-------------------------------------------------------------------
    private void configureClassicTelnet(MUDConnection connection) {
		MUDClientTelnet telnet = new MUDClientTelnet();
		telnet.setReversePipeline(streamToMUD);
		// GMCP
		setupGMCP();
		telnet.add(gmcp);
		telnet.add(new MUDServerStatusProtocol());
		ANSILayer ansi = new ANSILayer();
		
		telnet.setReversePipeline(streamToMUD);
		streamFromMUD
			.then(telnet)
			.then(ansi)
			;
		
		streamToMUD
			.then(ansi)
			.then(telnet)
			.then(connection)
			;
    }
    
	//-------------------------------------------------------------------
    private void configureMUDDown(MUDConnection connection) {
    	ClientMUDDownParser mudDownReceiver = new ClientMUDDownParser();
    	streamFromMUD
    		.then(mudDownReceiver);
    }

//	//-------------------------------------------------------------------
//	public MUDSession(TerminalEmulator terminal, MUDEventPipeline in, Config config) throws IOException {
//		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
//		this.console = terminal;
////		this.telnet  = builder.telnet;
//		this.world   = config.getServer().replaceAll("[^a-zA-Z0-9]", "_");
////		console.setLocalEchoActive(false);
////		console.setMode(TerminalMode.RAW);
//		
////		telnet.getInputStream().setSendGoAheadAsANSISepator(true);
//
////		console.getOutputStream().write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT520, true));
//		
//		streamToMUD   = new MUDEventPipeline("SND");
////		streamFromMUD.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> input: {0}={1}", k, v));
////		streamToMUD.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> output: {0}={1}", k, v));
//		
//		
////		setupECHO();
////		setupNAWS();
////		setupTTYPE(builder.terminalTypes);
////		setupMSP();
////		setupGMCP();
//		
//		streamFromMUD = terminal.connectWith(messageLog,in, new ByteArrayOutputStream());
//		if (config.isMXPEnabled()) {
//			setupMXP();
//		}
////		setupTranslator(config);
//		
//		// Is this a MUD that only sends LF, insteadt of CR LF
//		if (config.getDoesNotSendCR()!=null && config.getDoesNotSendCR()) {
//			streamFromMUD.addFilter(new LinefeedToCRLFFilter());
//		}
//		
//		terminal.start();
//	}
	
	//-------------------------------------------------------------------
	private void fireSessionChanged() {
		if (sessionListener!=null) sessionListener.accept(this);
	}
	
	//-------------------------------------------------------------------
	private void setupECHO() {
		logger.log(Level.INFO, "ENTER: setupECHO");
		echo = new EchoMode();
//		telnet.add(echo);
	}

	//-------------------------------------------------------------------
	private void setupNAWS() {
		logger.log(Level.INFO, "ENTER: setupNAWS");
		var naws = new TelnetWindowSize();
//		telnet.add(naws);
//		console.addConsoleSizeListener( size -> {
//			try {
//				logger.log(Level.INFO, "Console size changed to {0}x{1}", size[0],size[1]);
//				naws.update(telnet, size[0], size[1]);
//			} catch (IOException e) {
//				logger.log(Level.ERROR, "Failed sending NAWS update", e);
//			}
//		});
//		logger.log(Level.INFO, "LEAVE: setupNAWS");
	}
	
	//-------------------------------------------------------------------
	private void setupTTYPE(String[] terminalTypes) {
		logger.log(Level.INFO, "ENTER: setupTTYPE");
//		telnet.add(new TerminalType(terminalTypes!=null ? terminalTypes: new String[] {"xterm-256color"}));
	}
	
	//-------------------------------------------------------------------
	private void setupMXP() {
		logger.log(Level.INFO, "ENTER: setupMXP");
//		mxp = new MXPOption(CommunicationRole.CLIENT,"b");
//		mxp.addListener(new MXPListener() {
//			@Override
//			public void telnetMXPLearned(MxpSupportTable data) { }
//			@Override
//			public void mxpDTDChanged(String newDTD) {
//				fireSessionChanged();
//			}
//			@Override
//			public void mxpClientInfo(String client, String version, String mxpVersion, String style) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
//		telnet.add(mxp);
//		
//		mxpFilter = new MXPInputStreamFilter(mxp);
//		telnet.addListener( (TelnetListener)mxpFilter);
//		streamFromMUD.addFilter(mxpFilter);
	}
	
	//-------------------------------------------------------------------
	private void setupMSP() {
//		telnet.add(new MUDSoundProtocolOption(CommunicationRole.CLIENT));
//		console.getReadBuffer().addReadBufferHandler(new MSPHandler(this));
	}
	
	//-------------------------------------------------------------------
	private void setupGMCP() {
		logger.log(Level.INFO, "ENTER: setupGMCP");
		gmcp = new GMCPHandler(CommunicationRole.CLIENT, "Realm Runner", "0.0.1");
//		telnet.add(gmcp);
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
	public MUDSession withUserInterface(MUDSessionUserInterface ui) {
		streamFromMUD
			.then(ui)
			.then(ui.getTerminal())
			;

		ui.connectWithSession(this);
		this.console = ui.getTerminal();
		return this;
	}

	//-------------------------------------------------------------------
	public void start() {
		logger.log(Level.INFO, "ENTER: MUDSession.start");
		Objects.requireNonNull(ui, "MUDSessionUserInterface must be set before starting the session");
		Objects.requireNonNull(console, "Terminal must be set before starting the session");
		
		connection.start();
		console.start();
		
		StartEvent startEvent = new StartEvent();
		streamFromMUD.publish(startEvent);
		logger.log(Level.INFO, "LEAVE: MUDSession.start");
	}
	
	//-------------------------------------------------------------------
	public void close() {
		logger.log(Level.WARNING, "TODO: closing session");
		try {
			closed = true;
//			streamToMUD.close();
//			streamFromMUD.close();
//			socket.close();
			getConsole().close();
			
			if (translator!=null) translator.onConnectionLost();
			if (tts!=null) tts.onConnectionLost();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sessionListener.accept(this);
	}

//	@Override
//	public void optionStateChanged(TelnetOption extension, boolean active) {
//		// TODO Auto-generated method stub
//		logger.log(Level.WARNING, "Option: {0}={1}", extension, active ? " activated" : " deactivated");
//		if (extension==echo) {
//			console.setLocalEchoActive(!active);
//		}
//		
//		System.err.println("MUDSession.optionStateChanged: "+extension+"="+active);
//	}
//
//	@Override
//	public void telnetCommandReceived(TelnetCommand command) {
//		// TODO Auto-generated method stub
//		logger.log(Level.WARNING, "RCV Telnet command: {0}", command);
//		switch (command.getCode()) {
//		case GA:
//		case EOR:
//			console.releaseInputBuffer();
//			break;
//		}
//	}

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
	public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
		logger.log(Level.INFO, "MUDSession.apply: "+event.getClass().getSimpleName());
		return List.of();
	}

	@Override
	public List<PipeEvent> onSendToRemote(PipeEvent event) {
		// TODO Auto-generated method stub
		return List.of(event);
	}

//	@Override
//	public void telnetReady() {
//		// TODO Auto-generated method stub
//		
//	}

}
