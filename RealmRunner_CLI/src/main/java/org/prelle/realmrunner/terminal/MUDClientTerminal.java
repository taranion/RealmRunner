package org.prelle.realmrunner.terminal;

import static org.prelle.realmrunner.network.MainConfig.CONFIG_DIR;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.DeviceAttributes.OperatingLevel;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.CursorPosition;
import org.prelle.ansi.commands.EraseInDisplay;
import org.prelle.ansi.commands.EraseInLine;
import org.prelle.ansi.commands.LeftRightMarginMode;
import org.prelle.ansi.commands.ModeState;
import org.prelle.ansi.commands.SelectGraphicRendition;
import org.prelle.ansi.commands.SelectGraphicRendition.Meaning;
import org.prelle.ansi.commands.SetConformanceLevel;
import org.prelle.ansi.commands.SetLeftAndRightMargin;
import org.prelle.ansi.commands.SetTopAndBottomMargin;
import org.prelle.ansi.control.AreaControls;
import org.prelle.ansi.control.CursorControls;
import org.prelle.ansi.control.DisplayControl;
import org.prelle.ansi.control.ReportingControls;
import org.prelle.mud4j.gmcp.GMCPHandler;
import org.prelle.mud4j.gmcp.GMCPManager;
import org.prelle.mud4j.gmcp.Char.CharPackage;
import org.prelle.mud4j.gmcp.Char.Stats;
import org.prelle.mud4j.gmcp.Char.Vitals;
import org.prelle.mud4j.gmcp.Client.ClientMediaPackage;
import org.prelle.mud4j.gmcp.Client.ClientMediaPlay;
import org.prelle.mud4j.gmcp.Client.ClientPackage;
import org.prelle.mud4j.gmcp.Core.CorePackage;
import org.prelle.mud4j.gmcp.Room.GMCPRoomInfo;
import org.prelle.mud4j.gmcp.Room.RoomPackage;
import org.prelle.mud4j.gmcp.beip.BeipTilemapData;
import org.prelle.mud4j.gmcp.beip.BeipTilemapDef;
import org.prelle.mud4j.gmcp.beip.BeipTilemapInfo;
import org.prelle.mud4j.gmcp.beip.TilemapPackage;
import org.prelle.mudansi.CapabilityDetector;
import org.prelle.mudansi.MarkupElement;
import org.prelle.mudansi.MarkupParser;
import org.prelle.mudansi.MarkupType;
import org.prelle.mudansi.TerminalCapabilities;
import org.prelle.mudansi.UIGridFormat;
import org.prelle.mudansi.UIGridFormat.Area;
import org.prelle.mudansi.UIGridFormat.AreaDefinition;
import org.prelle.mudansi.UserInterfaceFormat;
import org.prelle.realmrunner.network.AbstractConfig;
import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.DataFileManager;
import org.prelle.realmrunner.network.LineBufferListener;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.MUDSessionGMCPListener;
import org.prelle.realmrunner.network.MainConfig;
import org.prelle.realmrunner.network.RRLogger;
import org.prelle.realmrunner.network.ReadFromConsoleTask;
import org.prelle.realmrunner.network.ReadFromMUDTask;
import org.prelle.realmrunner.network.SessionConfig;
import org.prelle.realmrunner.network.SessionConfig.SessionConfigBuilder;
import org.prelle.realmrunner.network.SoundManager;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetConstants.ControlCode;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSocket.State;
import org.prelle.telnet.TelnetSocketListener;
import org.prelle.telnet.mud.AardwolfMushclientProtocol.AardwolfMushclientListener;
import org.prelle.telnet.mud.AardwolfMushclientProtocol.MUDMode;
import org.prelle.telnet.option.TelnetWindowSize;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;
import org.prelle.terminal.console.UnixConsole;
import org.prelle.terminal.console.WindowsConsole;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.graphicmud.map.SymbolMap;
import com.graphicmud.symbol.SymbolSet;
import com.graphicmud.symbol.TileGraphicService;
import com.graphicmud.symbol.swing.SwingTileGraphicLoader;
import com.sun.jna.Platform;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

/**
 *
 */
public class MUDClientTerminal implements TelnetSocketListener, LineBufferListener,
	AardwolfMushclientListener, MUDSessionGMCPListener  {

	private static Logger logger ;
	private final static String AREA_ROOMDESC = "RoomDesc";

	private MainConfig mainConfig;
	private Config activeConfig;
	private MUDSession session;
	private TerminalCapabilities capabilities;
	private TerminalEmulator console;
	private Charset charset = StandardCharsets.US_ASCII;
	private UIGridFormat format;
	private ReadFromConsoleTask readFromConsole;
	private SoundManager sound;
	private TileGraphicService graphic;

	private Timer timer;
	private TimerTask updateNAWSTask;
	private int terminalWidth, terminalHeight;

	private Map<String, BeipTilemapDef> mapsByID = new HashMap<>();
	private Map<String, SymbolSet> setsByID = new HashMap<>();

	//-------------------------------------------------------------------
	public static void main(String[] args) throws Exception {
		System.setProperty("app.name", "RealmRunner");
		if (args.length==2) {
			String host = args[0];
			int    port = Integer.parseInt(args[1]);
			new MUDClientTerminal(null, host, port);
		} else if (args.length>0) {
			new MUDClientTerminal(args[0], null, 0);
		} else {
			new MUDClientTerminal("starmourn", null, 0);
		}
	}

	//-------------------------------------------------------------------
	public MUDClientTerminal(String world, String server, int port) throws Exception {
		setupLogging();
		logger.log(Level.INFO, "----------------------------------------------------\r\n");
		readConfig();
		logger.log(Level.DEBUG, "Configure file manager");
		DataFileManager.configure(mainConfig);
		sound = new JLayerSoundManager();

		AbstractConfig activeConfig = mainConfig;
		if (world!=null) {
			if (mainConfig.getWorld().containsKey(world))
				activeConfig = mainConfig.getWorld().get(world);
			else
				System.err.println("Unknown world '"+world+"'");
		} else if (server!=null) {
			activeConfig = new Config();
			((Config)activeConfig).setServer(server);
			((Config)activeConfig).setPort(port);
			((Config)activeConfig).setDataDir("dynamic");
			world = "dynamic";
		}

		if (Platform.isWindows()) {
			logger.log(Level.INFO, "Create a windows console");
			console = new WindowsConsole();
			charset = console.getEncodings()[1];
		} else {
			logger.log(Level.INFO, "Create a unix console");
			console = new UnixConsole();
			charset = console.getEncodings()[1];
		}
		logger.log(Level.DEBUG, "Console is {0} with charset {1}",console.getClass().getSimpleName(), charset);
		setupTimer();
		int[] size = console.getConsoleSize();
		logger.log(Level.DEBUG, "size is "+Arrays.toString(size));
		terminalHeight = size[1];
		terminalWidth  = size[0];

		console.getInputStream().setLoggingListener( (type,text) -> logger.log(Level.DEBUG, "CONSOLE <-- {0} = {1}", type,text));
		console.getOutputStream().setLoggingListener( (type,text) -> {if (!"PRINT".equals(type)) logger.log(Level.DEBUG, "CONSOLE --> {0} = {1}", type,text);});
		console.setLocalEchoActive(false);
		console.setMode(TerminalMode.RAW);
		readFromConsole = new ReadFromConsoleTask(console, activeConfig, (LineBufferListener)this);

		Thread readFromTerminal = new Thread(readFromConsole, "FromConsole");
		readFromTerminal.start();

		console.getOutputStream().write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT520, true));
		readFromConsole.setForwardMode(false);
		this.capabilities = new TerminalCapabilities();
		learnTerminal(readFromConsole);

//		DynamicallyRedefinableCharacterSet decdld = new DynamicallyRedefinableCharacterSet(
//				1,1,DynamicallyRedefinableCharacterSet.Erase.ALL_MATCHING_WIDTH_RENDITION,0,0,TextOrFullCell.FULL_CELL,0,0,
//				"???owYn||~ywo??/?IRJaVNn^NVbJRI");
//		console.getOutputStream().write(decdld);
//		console.getOutputStream().write( '!');
////		console.getOutputStream().write(new EscapeSequenceFragment("{ ", '@', "SCS0", org.prelle.ansi.Level.VT200));
//		console.getOutputStream().write(new DesignateCharacterSet(0, "P"));
//		console.getOutputStream().write( '!');
//		console.getOutputStream().write(new C0Fragment(C0Code.SI));
//		console.getOutputStream().write( '!');
//
//		console.getOutputStream().write(new Sixel(0, BackgroundMode.TRANSPARENT, List.of(
//				new RasterAttribute(1,1,20,20),
//				new SpecifyColor(0,2,100,0,0),
//				new SpecifyColor(1,2,0,0,100),
//				new SpecifyColor(2,2,0,100,0),
//				new UseColor(1),
//				new SixelData("~~@@vv@@~~@@~~$"),
//				new UseColor(2),
//				new SixelData("??}}GG}}??}}??"),
//				new NewLine(),
//				new Repeat(14,'@')
//				)));
//		console.getOutputStream().write("\r\r\n");
//
//		console.getOutputStream().flush();
//		System.exit(1);
		setupInterface();
		capabilities.report(console.getOutputStream());
		console.getOutputStream().write("\r\n");
		logger.log(Level.DEBUG, "---Interface all set up ---- now connect --------------------------");
//		console.setLocalEchoActive(true);
//		console.setMode(TerminalMode.LINE_MODE);
		readFromConsole.setForwardMode(true);

		/*
		 * Prepare telnet connection
		 */
		GMCPManager.registerPackage(new ClientMediaPackage());
		GMCPManager.registerPackage(new ClientPackage());
		GMCPManager.registerPackage(new RoomPackage());
		GMCPManager.registerPackage(new CorePackage());
		GMCPManager.registerPackage(new CharPackage());
		GMCPManager.registerPackage(new TilemapPackage());
		
		try {

			SessionConfigBuilder builder = SessionConfig.builder();
			if (activeConfig instanceof Config) {
				builder.server( ((Config)activeConfig).getServer());
				builder.port  ( ((Config)activeConfig).getPort());
				builder.login ( ((Config)activeConfig).getLogin());
				builder.passwd( ((Config)activeConfig).getPassword());

				DataFileManager.setActiveMUD(world, (Config)activeConfig);

			} else {
				builder.server("localhost").port(4000);
			}
			SessionConfig config = builder.build();
			logger.log(Level.INFO, "Starting the session");
//			setupSession(config, activeConfig);
		} catch (Exception e) {
			PrintStream pout = new PrintStream(console.getOutputStream());
			pout.append("Failed to connect: "+e);
			pout.flush();
		}
	}

	//-------------------------------------------------------------------
	private void setupLogging() {
		String homeDir = System.getProperty("user.home", "/tmp");
		CONFIG_DIR = Platform.isWindows()?
				Paths.get(homeDir, ".realmrunner")
				:
				Paths.get(homeDir, ".realmrunner");

		System.out.println("LOGFILE: "+RRLogger.LOGFILE);
		try {
			Files.createDirectories(MainConfig.CONFIG_DIR);
		} catch (IOException e) {e.printStackTrace();}
		logger = System.getLogger("mud.client");
	}

	//-------------------------------------------------------------------
	public void readConfig() throws FileNotFoundException {
		DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        // Fix below - additional configuration
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		Representer representer = new Representer(options) {
		    @Override
		    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,Tag customTag) {
		        // if value of property is null, ignore it.
		        if (propertyValue == null) {
		            return null;
		        }
		        else {
		            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
		        }
		    }
		};
		representer.addClassTag(MainConfig.class, Tag.MAP);

		// Reading
		TypeDescription configDesc = new TypeDescription(Charset.class);


		Yaml yaml = new Yaml(representer);
		Path configFile = CONFIG_DIR.resolve("config.yml");
		System.out.println("CONFIG : "+configFile.toAbsolutePath());
		logger.log(Level.DEBUG, "Try to read config from {0}",configFile.toAbsolutePath());
		try {
			if (!Files.exists(configFile)) {
				Files.createDirectories(configFile.getParent());
				Files.createFile(configFile);
			}
			mainConfig = (Files.exists(configFile))?yaml.loadAs(new FileReader(configFile.toFile()), MainConfig.class):(new MainConfig());
			if (mainConfig==null) {
				logger.log(Level.INFO, "Config exists, but is empty");
				mainConfig = new MainConfig();
				FileWriter out = new FileWriter(configFile.toFile());
				yaml.dump(mainConfig, out);
				out.flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Failed accessing or creating config at "+configFile);
			System.exit(1);
		}
	}

	//-------------------------------------------------------------------
	private void saveConfig() {
		DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        // Fix below - additional configuration
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		Representer representer = new Representer(options) {
		    @Override
		    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,Tag customTag) {
		        // if value of property is null, ignore it.
		        if (propertyValue == null) {
		            return null;
		        }
		        else {
		            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
		        }
		    }
		};
		representer.addClassTag(MainConfig.class, Tag.MAP);

		Yaml yaml = new Yaml(representer);
		Path configFile = CONFIG_DIR.resolve("config.yml");
		try {
			FileWriter out = new FileWriter(configFile.toFile());
			yaml.dump(mainConfig, out);
			out.flush();
			logger.log(Level.INFO, "Wrote config to {0}", configFile.toAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	private void learnTerminal(ReadFromConsoleTask readTask) {
		logger.log(Level.DEBUG, "ENTER: learnTerminal");
		logger.log(Level.INFO, "ENV = "+System.getenv("LC_ALL"));
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
			capabilities = detector.performCheck(terminalWidth, terminalHeight);
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
	private void installShutdown() {
		
		// What to do on shutdown
		Thread onShutdown = new Thread(() -> {
			logger.log(Level.WARNING, "Shutting down");
			System.err.println("Shutting down");
			if (session!=null) {
				logger.log(Level.INFO, "Closing MUD session");
				session.close();
			}
			// Resetting terminal
			ANSIOutputStream out = console.getOutputStream();
			logger.log(Level.INFO, "Resetting console");
			try {
				AreaControls.setLeftAndRightMargins(out, 1, 200);
				DisplayControl.setLeftRightMarginMode(out, ModeState.RESET);
				AreaControls.setTopAndBottomMargins(out, 1, 200);
				out.write(new SelectGraphicRendition(Meaning.BLINKING_ON));
				AreaControls.clearScreen(out);
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out.reset();
		});
		Runtime.getRuntime().addShutdownHook(onShutdown);

	}

	//-------------------------------------------------------------------
	private void setupSession(SessionConfig config, Config activeConfig) throws IOException, InterruptedException {
		session = new MUDSession(config, this, console.getConsoleSize());
		session.getSocket().setOptionListener(TelnetOption.MUSHCLIENT, (AardwolfMushclientListener)this);
		session.getSocket().setOptionListener(TelnetOption.MSP, sound);
		session.setGmcpListener(this);
		readFromConsole.setForwardTo(session.getSocket());
		
		Charset useCharset = charset;
		if (activeConfig.getServerEncoding()!=null)
			useCharset = Charset.forName(activeConfig.getServerEncoding());

		if ((activeConfig instanceof Config)  &&((Config)activeConfig).getServerEncoding()!=null) {
			console.getInputStream().setEncoding(useCharset);
		}

		ReadFromMUDTask readTask = new ReadFromMUDTask(session.getSocket(), console.getOutputStream(), activeConfig, useCharset);
		readTask.setControlSequenceFilter( frag -> filterFragmentFromMUD(frag));
		session.getStreamToMUD().setLoggingListener( (type,text) -> {if (!"PRINT".equals(type)) logger.log(Level.INFO, "MUD --> {0} = {1}", type,text);});
		Thread readThread = new Thread(readTask);
		readThread.start();

		if (config.getLogin()!=null) {
			session.getStreamToMUD().write( (config.getLogin()+"\r\n").getBytes(StandardCharsets.UTF_8));
			if (config.getPasswd()!=null) {
				session.getStreamToMUD().write( (config.getPasswd()+"\r\n").getBytes(StandardCharsets.UTF_8));
			}
		}
	}
	
	//-------------------------------------------------------------------
	private void setupTimer() {
		updateNAWSTask = new TimerTask() {
			public void run() {
				try {
					int[] size = console.getConsoleSize();
					boolean changed = size[0]!=terminalWidth || size[1]!=terminalHeight;
					terminalWidth = size[0];
					terminalHeight= size[1];
					if (changed ) {
						logger.log(Level.DEBUG, "Window size changed");
						if (capabilities!=null)
							setupInterface();
//						format.setSize(terminalWidth, terminalHeight);
//						format.recreate();
						if (session!=null) {
							session.sendWindowSizeUpdate(terminalWidth, terminalHeight);
							sendNAWS();
						}
					}
				} catch (Exception e) {
					logger.log(Level.ERROR, "Failed for NAWS update",e);
				}
			}
		};

		timer = new Timer("polling", true);
		timer.schedule(updateNAWSTask, 0, 500);
	}

	//-------------------------------------------------------------------
	private void setupInterface() {
		logger.log(Level.INFO, "###############################Set up a split screen interface");
		installShutdown();
		ANSIOutputStream out = console.getOutputStream();

		format = new UIGridFormat(out,terminalWidth, terminalHeight, capabilities.isEditRectangular());
		// Outer border only works when splitting on both axis
		format.setOuterBorder(capabilities.isMarginLeftRight() && capabilities.isMarginTopBottom());
		// Reserve space for the input line
		format.setBottomHeight(1);
//		format.setTopHeight(11);
//		format.setLeftWidth(22);
		format.join(UIGridFormat.ID_INPUT, Area.BOTTOM_LEFT, Area.BOTTOM, Area.BOTTOM_RIGHT);
		format.join(AREA_ROOMDESC, Area.TOP, Area.TOP_RIGHT);
		format.join(UIGridFormat.ID_SCROLL, Area.CENTER, Area.RIGHT);

		logger.log(Level.INFO, "Format.dump: "+format.dump());
		try {
			format.recreate(charset);
			CursorControls.enableCursor(out, false);
			lineBufferChanged("", 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.log(Level.INFO, "Done initializing");
//		System.exit(0);
		readFromConsole.setForwardMode(true);

		try {
			//out.write(new DeviceControlFragment("$", null, "qmrs"));
			logger.log(Level.DEBUG, "Request current margins");
			ReportingControls.requestTopBottomMargin(out);
//			AreaControls.clearScreen(out);
//			AreaControls.setLeftAndRightMargins(out, 13, terminalWidth);
////			AreaControls.setTopAndBottomMargins(out, 13, terminalWidth);
//			CursorControls.setCursorPosition(out, 13, 13);
			
			Path configFile = CONFIG_DIR.resolve("config.yml");
			out.write("Reading your configuration from "+configFile.toRealPath()+"\r\n");
			out.write("\r\n".repeat(20));
			out.write("Usage:\r\n"
					+ "#SESSION <name> <host> <port> [<charset>] - connect to the given server \r\n"
					+ "                                            Optionally define server <charset>\r\n"
					+ "#SESSION <name>               - connect to a stored server entry\r\n");
			out.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	private void sendNAWS() {
		if (session==null || session.getSocket()==null) return;
		AreaDefinition def = format.getArea(UIGridFormat.ID_SCROLL);
		logger.log(Level.DEBUG, "Real size is {0}x{1}, but tell server the size is {2}x{3}", terminalWidth, terminalHeight, def.getW(), def.getH());
		try {
			TelnetWindowSize.sendUpdate(session.getSocket(), def.getW(), def.getH());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetOptionStatusChange(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetOption, boolean)
	 */
	@Override
	public void telnetOptionStatusChange(TelnetSocket nvt, TelnetOption option, boolean active) {
		logger.log(Level.INFO, "Feature {0} is {1}", option, active?"enabled":"disabled");
		if (option==TelnetOption.ECHO) {
			console.setLocalEchoActive(!active);
			//console.setMode(TerminalMode.LINE_MODE)
		}
		if (option==TelnetOption.NAWS && active) {
			sendNAWS();
		}
		if (option==TelnetOption.GMCP && active) {
			try {
				GMCPManager.initiateAsClient(nvt.out());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mud4j.gmcp.Client.GMCPClientMediaListener#gmcpReceivedClientMedia(org.prelle.mud4j.gmcp.Client.ClientMediaPlay)
	 */
	@Override
	public void gmcpReceivedClientMedia(ClientMediaPlay play) {
		logger.log(Level.INFO, "Play {0} from {1}",play.name, play.url);
		String filename = play.name.replace(" ","%20");
		String url = play.url+"/"+filename;
		logger.log(Level.INFO, "-1-> "+url);
		URI uri = URI.create(url);
		logger.log(Level.INFO, "-2-> "+uri);

//		HttpClient client = HttpClient.newBuilder().build();
//		HttpRequest request = HttpRequest.newBuilder(uri)
//				.GET()
//				.header("User-Agent", "MUDClient")
//				.build();
		try {
			Path filePath = DataFileManager.downloadFileTo(play.name, uri);
			logger.log(Level.INFO, "Filepath = "+filePath);
			if (filePath!=null) {
				Player player = new Player(new FileInputStream(filePath.toFile()));
				Thread thread = new Thread( () -> {
					try {
						player.play();
					} catch (JavaLayerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				thread.start();
			}
		} catch (Exception e) {
			logger.log(Level.ERROR, "Failed downloading from "+uri+"\n"+e);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetCommandReceived(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetCommand)
	 */
	@Override
	public void telnetCommandReceived(TelnetSocket nvt, TelnetCommand command) {
//		logger.log(Level.INFO, "TODO: Command "+command);
		if (command.getCode()==ControlCode.GA) {
			try {
				console.getOutputStream().flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//-------------------------------------------------------------------
	public static String buildLine(List<MarkupElement> elements, int lineLength, boolean justify) {
		if (justify) {
			logger.log(Level.WARNING, "TODO: Implement justification");
		}
		StringBuffer ret = new StringBuffer();
		for (MarkupElement elem : elements) {
			switch (elem.getType()) {
			case TEXT: 	ret.append(elem.getText());  break;
			case SPACING: ret.append(' '); break;
			case STYLE:
			case COLOR:
				if (elem.isEndsMarkup()) {
					tagEnded(elem.getText(), ret);
				} else {
					tagStarted(elem.getText(), ret);
				}
				break;
//			case ENTITY:
//				if ("nbsp".equals(elem.getText())) {
//
//				}
//				ret.append(' '); break;
			case FLOW:
				if (elem.getText().equals("br")) {
//					ret.append("\r\n");
					continue;
				}
			default:
				logger.log(Level.WARNING, "No output for "+elem);
			}
		}
		return ret.toString();
	}

	//-------------------------------------------------------------------
	private static void tagStarted(String tagName, StringBuffer buf) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SelectGraphicRendition sgr = null;
		switch (tagName) {
		case "b":
			sgr = new SelectGraphicRendition(List.of(Meaning.BOLD_ON));
			break;
		case "blink":
			sgr = new SelectGraphicRendition(List.of(Meaning.BLINKING_ON));
			break;
		case "cyan":
			sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_CYAN));
			break;
		case "f":
			sgr = new SelectGraphicRendition(List.of(Meaning.FAINT_ON));
			break;
		case "green":
			sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_GREEN));
			break;
		case "i":
			sgr = new SelectGraphicRendition(List.of(Meaning.ITALIC_ON));
			break;
		case "n":
			sgr = new SelectGraphicRendition(List.of(Meaning.NEGATIVE_ON));
			break;
		case "red":
			sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_RED));
			break;
		case "u":
			sgr = new SelectGraphicRendition(List.of(Meaning.UNDERLINE_ON));
			break;
		default:
			logger.log(Level.WARNING, "Unhandled tag {0}", tagName);
			System.err.println("Unhandled start tag "+tagName);
		}

		if (sgr!=null) {
			sgr.encode(baos, true);
			buf.append(baos.toString(StandardCharsets.US_ASCII));
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @param tagName
	 * @param buf
	 * @return TRUE, if linebreak
	 */
	private static boolean tagEnded(String tagName, StringBuffer buf) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		SelectGraphicRendition sgr = null;
		switch (tagName) {
		case "b":
			sgr = new SelectGraphicRendition(List.of(Meaning.INTENSITY_OFF));
			break;
		case "blink":
			sgr = new SelectGraphicRendition(List.of(Meaning.BLINK_OFF));
			break;
		case "br":
			buf.append("\n");
			return true;
		case "cyan":
		case "green":
			sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_DEFAULT));
			break;
		case "f":
			sgr = new SelectGraphicRendition(List.of(Meaning.INTENSITY_OFF));
			break;
		case "i":
			sgr = new SelectGraphicRendition(List.of(Meaning.STYLE_OFF));
			break;
		case "n":
			sgr = new SelectGraphicRendition(List.of(Meaning.NEGATIVE_OFF));
			break;
		case "red":
			sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_DEFAULT));
			break;
		case "u":
			sgr = new SelectGraphicRendition(List.of(Meaning.UNDERLINE_OFF));
			break;
		default:
			logger.log(Level.WARNING, "Unhandled tag {0}", tagName);
			System.err.println("Unhandled end tag "+tagName);
		}

		if (sgr!=null) {
			sgr.encode(baos, true);
			buf.append(baos.toString(StandardCharsets.US_ASCII));
		}
		return false;
	}

	//-------------------------------------------------------------------
	public static List<String> convertText(List<MarkupElement> markup, int lineLength) {
		List<String> buf = new ArrayList<>();

		int line=0;
		List<MarkupElement> currentLine = new ArrayList<>();
		for (MarkupElement tmp : markup) {
//			logger.log(Level.INFO, tmp);
			MarkupElement last = (currentLine.isEmpty())?null:currentLine.getLast();
			if (last!=null && last.getType()==MarkupType.TEXT) {
				currentLine.add(new MarkupElement(last));
			}

			switch (tmp.getType()) {
			case TEXT:
				List<MarkupElement> textElements = currentLine.stream().filter(e -> e.getType()==MarkupType.TEXT).toList();
				int currentTextLength = (int) textElements.stream()
					.map(elem -> elem.getLength())
					.reduce(0,  Integer::sum);
				// Minimal spaces
				currentTextLength+=textElements.size();
				// Will it fit in the line?
				if ( (currentTextLength+tmp.getLength())<=lineLength) {
					// Fits
					currentLine.add(tmp);
				} else {
					// Won't fit
					// Existing line will be written
//					logger.log(Level.WARNING, "Wont fit: {0} + {1} > {2}", currentTextLength, tmp.getLength(), lineLength);
					buf.add(buildLine(currentLine, lineLength, false));
					currentLine.clear();
					currentLine.add(tmp);
				}
				break;
			case FLOW:
				if (tmp.getText().equals("br")) {
					buf.add(buildLine(currentLine, lineLength, false));
					currentLine.clear();
				}
			default:
				currentLine.add(tmp);
			}
		}
		if (!currentLine.isEmpty()) {
//			logger.log(Level.INFO, "Remain "+currentLine);
			buf.add(buildLine(currentLine, lineLength, false));
		}

		return buf;
	}

	//-------------------------------------------------------------------
	public static String convertTextBlock(List<MarkupElement> markup, int lineLength) {
		return String.join("\r\n", convertText(markup,lineLength));
	}

	//-------------------------------------------------------------------
	protected String makeHeaderLine(GMCPRoomInfo room) {
		StringBuffer output = new StringBuffer();
		List<MarkupElement> nameMarkup = MarkupParser.convertText(room.getName());
		String title = convertTextBlock(nameMarkup, terminalWidth);
		if (title!=null) {
			output.append(title);
			output.append(" ");
		}

		if (room.getExits()!=null) {
			StringBuffer toConvert = new StringBuffer("<cyan> ");
			for (Entry<String,Integer> entry : room.getExits().entrySet()) {
				toConvert.append(entry.getKey().toUpperCase().charAt(0) );
				toConvert.append(' ');
			}
			toConvert.append("</cyan>");
			output.append( convertText(MarkupParser.convertText(toConvert.toString()),20));
		}

		return output.toString();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mud4j.gmcp.Room.GMCPRoomListener#gmcpReceivedRoomInfo(org.prelle.mud4j.gmcp.Room.GMCPRoomInfo)
	 */
	@Override
	public void gmcpReceivedRoomInfo(GMCPRoomInfo info) {
		logger.log(Level.INFO, "Room "+info.getName());
		ANSIOutputStream out = console.getOutputStream();

		StringBuffer buf = new StringBuffer("<b><u>");
		buf.append(info.getName());
		buf.append("</b></u>");
		// Exits
		if (info.getExits()!=null) {
			buf.append("  [<cyan>");
			buf.append( String.join(" ", info.getExits().keySet().stream().map(k -> k.toUpperCase()) .toList()) );
			buf.append("</cyan>]");
		}
		buf.append("<br/>");

		// Description
		if (info.getDesc()!=null) {
			buf.append(info.getDesc());
		}


		try {
			if (format.getArea(AREA_ROOMDESC)!=null)
				format.showMarkupIn(AREA_ROOMDESC, buf.toString(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (true)
			return;

		int mapHeight=11;
		int mapWidth=11;
		int columns=terminalWidth;
		List<MarkupElement> description = MarkupParser.convertText(info.getDesc());

		try {
			CursorControls.savePositionDEC(out);
			// Clear old content
			for (int i=0; i<mapHeight; i++) {
				CursorControls.setCursorPosition(out, mapWidth+3, i+2);
				AreaControls.clearFromHere(out);
				CursorControls.setCursorPosition(out, columns, i+2);
				out.write("\u2551");
			}

			CursorControls.setCursorPosition(out, mapWidth+3, 2);
			out.write(makeHeaderLine(info));
//		CursorControls.setCursorPosition(out, columns, 2);
//		out.write("\u2551");

			// Write room description
			List<String> roomDesc = convertText(description, columns-mapWidth -3);
			int y = 3;
			for (String line : roomDesc) {
				CursorControls.setCursorPosition(out, mapWidth+3, y++);
				out.write(line.getBytes(StandardCharsets.ISO_8859_1));
			}
			CursorControls.restorePositionDEC(out);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetSocketChanged(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetSocket.State, org.prelle.telnet.TelnetSocket.State)
	 */
	@Override
	public void telnetSocketChanged(TelnetSocket nvt, State oldState, State newState) {
		logger.log(Level.DEBUG, "state changed "+newState);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.mud.AardwolfMushclientProtocol.AardwolfMushclientListener#telnetMudModeChanged(org.prelle.telnet.mud.AardwolfMushclientProtocol.MUDMode)
	 */
	@Override
	public void telnetMudModeChanged(MUDMode mode) {
		logger.log(Level.WARNING, "TODO: MUSHCLIENT mode changed to {0}", mode);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.mud.AardwolfMushclientProtocol.AardwolfMushclientListener#telnetTickReceived()
	 */
	@Override
	public void telnetTickReceived() {
		logger.log(Level.WARNING, "TODO: MUSHCLIENT TICK");
	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mud4j.gmcp.Char.GMCPCharListener#gmcpReceivedVitals(org.prelle.mud4j.gmcp.Char.Vitals)
	 */
	@Override
	public void gmcpReceivedVitals(Vitals value) {
		logger.log(Level.WARNING, "TODO: GMCP Vitals "+value);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mud4j.gmcp.Char.GMCPCharListener#gmcpReceivedStats(org.prelle.mud4j.gmcp.Char.Stats)
	 */
	@Override
	public void gmcpReceivedStats(Stats value) {
		logger.log(Level.WARNING, "TODO: GMCP Stats "+value);
	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.LineBufferListener#lineBufferChanged(java.lang.String, int)
	 */
	@Override
	public void lineBufferChanged(String content, int cursorPosition) {
//		logger.log(Level.INFO, "##lineBufferChanged({0}, {1})", content, cursorPosition);
		try {
			CursorControls.savePositionDEC(console.getOutputStream());
			format.showLinebuffer(content, false);
			console.getOutputStream().write(new SelectGraphicRendition(Meaning.BLINKING_ON));
			console.getOutputStream().write("\u2588");
			console.getOutputStream().write(new SelectGraphicRendition(Meaning.BLINK_OFF));
			CursorControls.restorePositionDEC(console.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.LineBufferListener#processCommandTyped(java.lang.String)
	 */
	@Override
	public String processCommandTyped(String typed) {
		logger.log(Level.WARNING, "processCommandTyped("+typed+") ... TODO: local aliases");
		if (typed.trim().startsWith("#")) {
			performLocalCommand(typed.stripLeading().substring(1));
			return null;
		}
		return typed;
	}

	//-------------------------------------------------------------------
	private AParsedElement filterFragmentFromMUD(AParsedElement frag) {
		if (Boolean.TRUE==mainConfig.getServerLayoutControl())
			return frag;
		
		if (frag instanceof EraseInDisplay) {
			logger.log(Level.WARNING, "Replace EraseInDisplay with clearing area");
			AreaDefinition area = format.getArea(UIGridFormat.ID_SCROLL);
			try {
				format.clear(area);
			} catch (Exception e) {
				logger.log(Level.ERROR, "IOException clearing area",e);
			}
			return null;
		}
		if (frag instanceof EraseInLine) {
			logger.log(Level.WARNING, "Ignore EraseInLine ");
			return null;
		}
		if (frag instanceof CursorPosition) {
			AreaDefinition scroll = format.getArea(UIGridFormat.ID_SCROLL);
			CursorPosition cup = (CursorPosition)frag;
			CursorPosition newCup = new CursorPosition(cup.getColumn() + scroll.getX(), cup.getLine() + scroll.getY());
			logger.log(Level.WARNING, "Replace CursorPosition {0} with {1}", cup, newCup);
			return newCup;
		}
		if (frag instanceof SetLeftAndRightMargin || frag instanceof SetTopAndBottomMargin || frag instanceof LeftRightMarginMode) {
			logger.log(Level.WARNING, "Ignore "+frag.getClass().getSimpleName());
			return null;
		}
		if (frag instanceof SelectGraphicRendition)
			return frag;
		if (frag instanceof PrintableFragment)
			return frag;
		if (frag instanceof C0Fragment)
			return frag;
		logger.log(Level.WARNING, "No filter for "+frag);
		return frag;
	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mud4j.gmcp.beip.GmcpBeipTilemapListener#gmcpBeipTilemapInfo(org.prelle.mud4j.gmcp.beip.BeipTilemapInfo)
	 */
	@SuppressWarnings("exports")
	@Override
	public void gmcpBeipTilemapInfo(BeipTilemapInfo info) {
		logger.log(Level.INFO, "gmcpBeipTilemapInfo");

		int id=0;
		for (Entry<String, BeipTilemapDef> entry : info.entrySet()) {
			mapsByID.put(entry.getKey(), entry.getValue());
			SymbolSet set = new SymbolSet(++id);

			try {
				URI uri = URI.create(entry.getValue().tileUrl);
				set.setFile( DataFileManager.downloadFileTo("tilesets/"+uri.getPath(), uri) );
				set.setTileSize(entry.getValue().getTileWidth());
				set.setTitle(entry.getKey());
				setsByID.put(entry.getKey(), set);
				logger.log(Level.INFO, "Symbols for ''{0}'' can be found in {1}", entry.getKey(), set.getFile());
				if (graphic!=null) {
					graphic.loadSymbolImages(set);
				} else {
					logger.log(Level.ERROR,"Graphic stack not initialized");
				}
			} catch (Exception e) {
				logger.log(Level.ERROR, "Error loading tileset",e);
			}
		}
	}

	@Override
	@SuppressWarnings("exports")
	public void gmcpBeipTilemapUpdate(BeipTilemapData data) {
		logger.log(Level.INFO, "gmcpBeipTilemapUpdate");
		if (!capabilities.isInlineImageKitty()) {
			logger.log(Level.WARNING, "Ignore Beip tilemap because no KiTTY inline image support found");
			return;
		}

		try {
			for (Entry<String, String> entry : data.entrySet()) {
				logger.log(Level.INFO, "{0}={1}", entry.getKey(), entry.getValue());
				BeipTilemapDef def = mapsByID.get(entry.getKey());
				if (def==null) {
					logger.log(Level.ERROR, "Got map for ''{0}'' but cannot find a definition", entry.getKey());
					continue;
				}
				SymbolSet set = setsByID.get(entry.getKey());
				if (set==null) {
					logger.log(Level.ERROR, "Got map data for ''{0}'' but cannot find tileset", entry.getKey());
					continue;
				}

				logger.log(Level.INFO, "Length of chars = "+entry.getValue().length());
				int[][] mapData = decodeBeipMap(entry.getValue(), def.getMapRows(), def.getMapColumns());
				SymbolMap symMap = new SymbolMap(mapData, set);
				byte[] pngData = graphic.renderMap(symMap, set);
				logger.log(Level.INFO, "Converted to "+pngData.length+" bytes of PNG");
				format.sendKittyImage(Area.TOP_LEFT.name(), pngData);
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, "Error sending KITTY image",e);
		}
	}

	//-------------------------------------------------------------------
	private  static int[][] decodeBeipMap(String encoded, int rows, int columns) {
		logger.log(Level.INFO, "decodeBeiMap( {0}, {1})", rows, columns);
	    int[][] map = new int[rows][columns];
	    int index = 0;

	    for (int y = 0; y < rows; y++) {
	        for (int x = 0; x < columns; x++) {
	            // Extrahiere 2 Zeichen (1 Hexadezimalzahl) aus dem StringBuffer
	            String hexValue = encoded.substring(index, index + 2);
	            // Konvertiere den Hexadezimalwert in einen Integer
	            map[y][x] = Integer.parseInt(hexValue, 16);
	            // Erhöhe den Index um 2 für die nächsten 2 Zeichen
	            index += 2;
	        }
	    }

	    return map;
	}

	//-------------------------------------------------------------------
	private void performLocalCommand(String value) {
		logger.log(Level.WARNING, "TODO: performLocalCommand({0})", value);
		StringTokenizer tok = new StringTokenizer(value);
		String command = tok.nextToken().toUpperCase();
		try {
			switch (command) {
			case "SESSIONS":
				performSessions(tok);
				return;
			case "SESSION":
				performSession(tok);
				return;
			case "QUIT":
				System.exit(0);
			default:
				logger.log(Level.ERROR, "Unknown command {0}", command);
				console.getOutputStream().write("Unknown command #"+command+"\r\n");
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, "Failed executing command '"+command+"'",e);
		}
	}

	//-------------------------------------------------------------------
	private void performSession(StringTokenizer tok) throws IOException {
		activeConfig = new Config(mainConfig);
		String world = null;
		switch (tok.countTokens()) {
		case 1:
			// Connect to existing server
			world = tok.nextToken();
			if (mainConfig.getWorld().containsKey(world))
				activeConfig = mainConfig.getWorld().get(world);
			else {
				console.getOutputStream().write("Unknown world '"+world+"'\r\n");
				return;
			}
			break;
		case 3:
		case 4:
			// Set up a new server
			world      = tok.nextToken();
			String host= tok.nextToken();
			String port= tok.nextToken();
			String enc = (tok.hasMoreTokens())?tok.nextToken():null;
			activeConfig = new Config();
			activeConfig.setServer(host);
			activeConfig.setPort(Integer.parseInt(port));
			activeConfig.setServerEncoding(enc);
			if (enc!=null) {
				try {
					Charset.forName(enc);
				} catch (Exception e) {
					// Invalid charset
					console.getOutputStream().write("Invalid character encoding: "+enc+"\r\n");
					return;
				}
			}
			mainConfig.addWorld(world, activeConfig);
			saveConfig();
			break;
		default:
			console.getOutputStream().write("Usage:\r\n"
					+ "#SESSION <name> <host> <port> [<charset>] - connect to the given server \r\n"
					+ "                                            Optionally define server <charset>\r\n"
					+ "#SESSION <name>               - connect to a stored server entry\r\n");
			return;
		}
		// Do connect
		logger.log(Level.INFO, "Establish session with {0}", world);
		format.clear(format.getArea(UIGridFormat.ID_SCROLL));
		DataFileManager.setActiveMUD(world, activeConfig);
		if (activeConfig.getServerLayoutControl()!=null && activeConfig.getServerLayoutControl()==true) {
			logger.log(Level.INFO, "Clear all layout");
			format.reset();
		}
		try {
			SessionConfig config = SessionConfig.builder()
				.server(activeConfig.getServer())
				.port(activeConfig.getPort())
				.build();
			Path dataDir = DataFileManager.getCurrentDataDir().resolve(world);
			Files.createDirectories(dataDir);
			activeConfig.setDataDir(dataDir.toString());
			graphic = new SwingTileGraphicLoader(DataFileManager.getCurrentDataDir().resolve("tilesets"));
			logger.log(Level.INFO, "Starting the session to {0} with dir {1}", activeConfig.getServer(), dataDir);
			setupSession(config, activeConfig);
		} catch (Exception e) {
			logger.log(Level.ERROR, "Failed to connect",e);
			PrintStream pout = new PrintStream(console.getOutputStream());
			pout.append("Failed to connect: "+e);
			pout.flush();
		}

		
	}

	//-------------------------------------------------------------------
	private void performSessions(StringTokenizer tok) throws IOException {
		List<Entry<String,Config>> worlds = mainConfig.getWorlds();
		worlds = worlds.stream().sorted( (w1,w2) -> w1.getKey().compareTo(w2.getKey())).toList();
		StringBuffer buf = new StringBuffer();
		for (Entry<String,Config> entry : worlds) {
			buf.append(String.format("%10s : %s, Port %d\r\n", entry.getKey(), entry.getValue().getServer(), entry.getValue().getPort()));
		}
		console.getOutputStream().write(buf.toString());
	}

}

