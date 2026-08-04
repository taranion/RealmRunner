package org.prelle.mudclient.jfx;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.commands.DeviceAttributes;
import org.prelle.ansi.commands.DeviceAttributes.Variant;
import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.DataFileManager;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.MainConfig;
import org.prelle.realmrunner.network.SoundManager;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.graphicmud.Localization;
import com.graphicmud.symbol.DefaultSymbolManager;
import com.graphicmud.symbol.SymbolManager;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 */
public class MUDClientMain extends Application {

	private final static Logger logger = System.getLogger("MUDClientMain");

	static record HistoryEntry(String text, Node node) {
	}

	private MainConfig mainConfig;

	private SymbolManager symbols;

	private WelcomePane welcome;
	private TabPane tabs;
	
	private List<HistoryEntry> history;
	private Session session;
	private Map<MUDSession, Tab> sessionTabs;
	
	private BooleanProperty darkMode = new SimpleBooleanProperty(false);
	private BooleanProperty musicEnabled = new SimpleBooleanProperty(true);
	private BooleanProperty soundEnabled = new SimpleBooleanProperty(true);
	private SwitchIconButton btnTheme, btnMusic, btnSound;
	
	private SoundManager soundManager;

	//-------------------------------------------------------------------
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Application.launch(args);

	}

	//-------------------------------------------------------------------
	/**
	 * @see javafx.application.Application#init()
	 */
	@Override
	public void init() {
		history = new ArrayList<MUDClientMain.HistoryEntry>();
		sessionTabs = new LinkedHashMap<>();
		symbols = new DefaultSymbolManager(
				Paths.get("/home/prelle/git/MUD2024/Example MUD","src/main/resources/static/symbols"),
				new com.graphicmud.symbol.jfx.JavaFXTileGraphicLoader());
		try {
			readConfig();
			DataFileManager.configure(mainConfig);
			soundManager = new JavaFXSoundManager();
			
			Localization.addPropertiesFrom("Localization");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		btnMusic = new SwitchIconButton(musicEnabled, "musical-note.png", "no-music.png", "Toggle game music");
		btnSound = new SwitchIconButton(soundEnabled, "sound-waves.png", "no-sound.png", "Toggle game sounds");
		btnTheme = new SwitchIconButton(darkMode, "day-mode.png", "night-mode2.png", "Toggle light/dakr theme");
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		darkMode.addListener( (_, _, newVal) -> {
			if (newVal) {
				Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
			} else {
				Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
			}
		});
		
		btnTheme.setOnAction( (_) -> {
			darkMode.set(!darkMode.get());
		});
		btnMusic.setOnAction( (_) -> {
			musicEnabled.set(!musicEnabled.get());
		});
		btnSound.setOnAction( (_) -> {
			soundEnabled.set(!soundEnabled.get());
		});
	}

	//-------------------------------------------------------------------
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage stage) throws Exception {
        // find more themes in 'atlantafx.base.theme' package
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        //Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
		tabs = new TabPane();
		// Add welcome pane
		welcome = new WelcomePane( (cfg) -> connectWith(cfg, stage));
		var mainTab = new Tab("New", welcome);
		mainTab.setClosable(false);
		tabs.getTabs().add(mainTab);
		
		welcome.setData(mainConfig);
		
		var button = new Button("My button");
		initComponents();
		initInteractivity();
        HeaderBar.setAlignment(button, Pos.CENTER_LEFT);
        HeaderBar.setMargin(button, new Insets(5));
        var headerBar = new HeaderBar();
        headerBar.setLeft(new Label("RealmRunner"));
        headerBar.setCenter(button);
        headerBar.setRight(new HBox(0,btnSound,btnMusic,btnTheme));

        var root = new BorderPane();
        root.setStyle("-fx-background: #afafbf;");
        root.setTop(headerBar);
        root.setCenter(tabs);


		Scene scene = new Scene(root, 1000,1200);
		stage.setScene(scene);
		stage.setWidth(1000);
       
        stage.initStyle(StageStyle.EXTENDED);
		stage.show();

//		Stage dialogStage = new Stage();
//		ConnectionDialog choices = new ConnectionDialog(mainConfig);
//		Scene dialogScene = new Scene(choices);
//		dialogStage.setScene(dialogScene);
//		dialogStage.showAndWait();
//
//		Config connectWith = choices.getSelected();
//		logger.log(Level.DEBUG, "Connect to {0}", connectWith);
//		if (connectWith!=null) {
//			connectWith(connectWith);
//		}
		
//		Config connectWith = mainConfig.getWorld().get("eden");
//		connectWith(connectWith);
		
		Platform.accessibilityActiveProperty().addListener( (_,_,n ) -> {
			logger.log(Level.INFO, "Accessibility is now {0}", n?"active":"inactive");
		});
	}

//	//-------------------------------------------------------------------
//	public void connect(MUDConnection connection) {
//		logger.log(Level.INFO, "Connecting to MUD {0}", connection);
//		try {
//			TelnetProtocol protocol = new TelnetProtocol(CommunicationRole.CLIENT)
//					.add(new TelnetCharset(null, "UTF-8","ASCII"))
//					.add(new TerminalType("ghostty","ghostty-xterm"))
//					.add(new MXPOption("b"))
//					;
//			
//			TelnetOutputStream tout = new TelnetOutputStream(connection.getStreamToMUD(), protocol);
//			TelnetInputStream   tin = new TelnetInputStream(connection.getStreamFromMUD(), protocol);
//			tin.setReverseStream(tout);
//			
//			MXPInputStreamFilter mxpFilter = new MXPInputStreamFilter();
//			
//			ANSIInputStream in = new ANSIInputStream(tin);
//			in.addFilter(mxpFilter);
//			// TODO: create config option "assumeCRbeforeLF" - some MUDs send only LF, but we want CRLF for the terminal
//			if (true)
//				in.addFilter(new LinefeedToCRLFFilter());
//			
//			protocol.addListener(new TelnetListener() {
//				@Override
//				public void telnetCommandReceived(TelnetCommand command) {
//					if (command.getCode()==ControlCode.GA) {
//						logger.log(Level.INFO, "Telnet command received: {0}", command);
//						in.releaseBuffer();
//					}
//				}
//				@Override
//				public void optionStateChanged(TelnetSubnegotiationHandler extension, boolean active) {
//					if (extension instanceof MXPOption) {
//						logger.log(Level.INFO, "MXP option state changed: {0} = {1}", extension, active);
//						mxpFilter.setMXPActive(active);
//					}
//				}
//			});
//			
//			console.connectWith(in, tout);
//			
//			protocol.initializeExtensions();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}


//	private MUDSession startReadingFromMUD(SessionConfig config, Config activeConfig) throws IOException, InterruptedException {
//		Charset useCharset = StandardCharsets.UTF_8;
//		if (activeConfig.getServerEncoding()!=null)
//			useCharset = Charset.forName(activeConfig.getServerEncoding());
//
//
//		MUDSession session = new MUDSession(config,  console.getConsoleSize(), useCharset);
//		session.getSocket().setOptionListener(WellKnownTelnetOptions.MUSHCLIENT, (AardwolfMushclientListener)this);
////		session.getSocket().setOptionListener(TelnetOption.MSP, sound);
////		session.setGmcpListener(this);
//
//		if ((activeConfig instanceof Config)  &&((Config)activeConfig).getServerEncoding()!=null) {
//			console.getInputStream().setEncoding(useCharset);
//		}
//
//		logger.log(Level.INFO, "Read from MUD with charset {0}", useCharset);
//		ReadFromMUDTask readTask = new ReadFromMUDTask(session.getSocket(), console.getOutputStream(), activeConfig, useCharset);
//		readTask.setControlSequenceFilter( frag -> filterFragmentFromMUD(frag));
//		session.getStreamToMUD().setLoggingListener( (type,text) -> {if (!"PRINT".equals(type)) logger.log(Level.INFO, "MUD --> {0} = {1}", type,text);});
//		Thread readThread = new Thread(readTask);
//		readThread.start();
//
//		return session;
//	}
//
//	//-------------------------------------------------------------------
//	private AParsedElement filterFragmentFromMUD(AParsedElement frag) {
//		return frag;
//	}
//
//	//-------------------------------------------------------------------
//	private ReadFromConsoleTask startReadingFromTerminal(Config activeConfig) throws IOException {
//		ReadFromConsoleTask readFromConsole = new ReadFromConsoleTask(console, activeConfig, null);
//
//		Thread readFromTerminal = new Thread(readFromConsole, "FromConsole");
//		readFromTerminal.start();
//		return readFromConsole;
//	}

//	//-------------------------------------------------------------------
//	/**
//	 * @param connectWith
//	 */
//	private void connectWith(Config connectWith) {
//		Thread thread = new Thread(() -> {
//			logger.log(Level.INFO, "Now create session");
//			try {
//	//			session = SessionManager.createSession("rom.mud.de", 4000);
//	//			session = SessionManager.createSession("mg.mud.de", 4711);
//				session = SessionManager.createSession(connectWith.getServer(), connectWith.getPort());
//	//			session = SessionManager.createSession("localhost", 4000);
//				InetAddress host = InetAddress.getByName("rom.mud.de");
//				MUDConnection con = new TCPMUDConnection(host, 4000);
////				connect(con);
////				MUDConnection con = new WebsocketMUDConnection(host, 4002);
//				connect(con);
//				
////				Thread readFromMUD = new Thread(new InputStreamThread(con.getStreamFromMUD(), console.getOutputStream()));
////				readFromMUD.start();
//
////				session.connect(new SessionListener() {
////
////					@Override
////					public void textReceived(String msg) {
////						System.out.println("-----\n"+msg);
////						terminal.getTerminal().write(msg);
////						try {
////							console.getOutputStream().write(msg);
////						} catch (IOException e) {
////							// TODO Auto-generated catch block
////							e.printStackTrace();
////						}
//////						try {
////////							terminalWriter.write(msg);
////////							ttyConnector.write(msg);
//////						} catch (IOException e) {
//////							// TODO Auto-generated catch block
//////							e.printStackTrace();
//////						}
//////				        Platform.runLater( () -> {
//////				        	Node pane = FlowBuilder.configure()
//////				        			.fontFamily("Monospaced Regular")
//////				        			.fontSize(12)
//////				        			.darkMode(false)
//////				        		.message(msg)
//////				        		.build();
//////				        	HistoryEntry entry = new HistoryEntry(msg, pane);
//////				        	history.add(entry);
//////				        	historyPane.getChildren().add(pane);
//////				        	if (historyPane.getChildren().size()>20) {
//////				        		historyPane.getChildren().remove(0);
//////				        		history.remove(0);
//////				        	}
//////				        	//scroll.setVvalue(1.0);
//////				        	});
////					}
////
////					@Override
////					public void connectionLost(Session session) {
////						System.exit(0);
////					}
////
////					@Override
////					public void mapReceived(org.prelle.telnet.mud.MUDTilemapProtocol.TileMapData data) {
////						// TODO Auto-generated method stub
////						mapView.setData(data.getRawData());
////					}
////				});
//
////				ReadFromConsoleTask readFromConsole = startReadingFromTerminal(connectWith);
////				MUDSession session = startReadingFromMUD(null, connectWith);
////				MUDSession session = MUDSession.builder(console)
////						.setCharset(StandardCharsets.UTF_8)
////						.build();
//				//session.co
//
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
//		thread.start();
//
//	}
//
//	//-------------------------------------------------------------------
//	private void sendInput(String text) {
//		console.sendUserInput(text);
//	}

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



		Yaml yaml = new Yaml(representer);
		String homeDir  = System.getProperty("user.home", "/tmp");
		Path configDir  = Paths.get(homeDir, ".realmrunner");
		Path configFile = configDir.resolve("config.yml");
		MainConfig.CONFIG_DIR=configDir;
		System.out.println("Try to read config from "+configFile.toAbsolutePath());
		try {
			mainConfig = (Files.exists(configFile))?yaml.loadAs(new FileReader(configFile.toFile()), MainConfig.class):(new MainConfig());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Main config = "+mainConfig);
	}

	//-------------------------------------------------------------------
	private void connectWith(Config connectWith, Stage stage) {
		logger.log(Level.INFO, "Connect with {0}", connectWith);
		
		MUDSessionUserInterfaceJFX ui = new MUDSessionUserInterfaceJFX();
		
		// Start a new MUDSession
		try {
			MUDSession session = MUDSession.builder(ui.getTerminal())
					.setClientConfig(connectWith)
					.setTerminalTypes("Realm Runner","xterm","MTTS 271")
					.build();
			if (session!=null) {
				Tab tab = new Tab(connectWith.getServer(), ui);
				tab.setOnClosed(_ -> {
					logger.log(Level.INFO, "User is closing tab for session {0}", session);
					session.close();
					sessionTabs.remove(session);
				});
				tabs.getTabs().add(tab);
				tabs.getSelectionModel().select(tab);
				sessionTabs.put(session, tab);
				ui.setSession(session);
			}
		} catch (UnknownHostException e) {
			    var alert = new Alert(AlertType.ERROR);
			    alert.setTitle("Error Dialog");
			    alert.setHeaderText("Unknown host");
			    alert.setContentText("The specified host could not be resolved: "+connectWith.getServer());
			    alert.initOwner(stage.getOwner());
			    alert.show();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
