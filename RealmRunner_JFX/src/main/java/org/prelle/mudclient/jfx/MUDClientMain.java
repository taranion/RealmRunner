package org.prelle.mudclient.jfx;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.commands.DeviceAttributes;
import org.prelle.ansi.commands.DeviceAttributes.Variant;
import org.prelle.fxterminal.TerminalView;
import org.prelle.jeditermfxterminal.GhosttyTerminalView;
import org.prelle.jeditermfxterminal.SwitchableInputStream;
import org.prelle.jeditermfxterminal.SwitchableOutputStream;
import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.DataFileManager;
import org.prelle.realmrunner.network.InputStreamThread;
import org.prelle.realmrunner.network.MUDConnection;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.MainConfig;
import org.prelle.realmrunner.network.ReadFromConsoleTask;
import org.prelle.realmrunner.network.ReadFromMUDTask;
import org.prelle.realmrunner.network.SessionConfig;
import org.prelle.realmrunner.network.TCPMUDConnection;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.mud.AardwolfMushclientProtocol.AardwolfMushclientListener;
import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.Terminal.Size;
import org.prelle.terminal.emulated.delete.Emulation;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.graphicmud.symbol.DefaultSymbolManager;
import com.graphicmud.symbol.SymbolManager;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 */
public class MUDClientMain extends Application {

	private final static Logger logger = System.getLogger("MUDClientMain");

	static record HistoryEntry(String text, Node node) {
	}

	private MainConfig mainConfig;

	private SymbolManager symbols;

	private ScrollPane scroll;
	private VBox historyPane;
	private TextField tfInput;
	private VBox textLayout;
	private TerminalView terminal;
	private GhosttyTerminalView console;
	private MapView mapView;
	private VBox mapLayout;
	private HBox layout;

	private List<HistoryEntry> history;
	private Session session;

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
		symbols = new DefaultSymbolManager(
				Paths.get("/home/prelle/git/MUD2024/Example MUD","src/main/resources/static/symbols"),
				new com.graphicmud.symbol.jfx.JavaFXTileGraphicLoader());
		try {
			readConfig();
			DataFileManager.configure(mainConfig);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage stage) throws Exception {
		historyPane = new VBox();
		historyPane.setSpacing(0);
		historyPane.setMinSize(960,400);

		scroll = new ScrollPane(historyPane);
		scroll.setMaxHeight(400);
		scroll.setMinWidth(960);
		scroll.setFitToWidth(true);
		scroll.vvalueProperty().bind(historyPane.heightProperty());

		Terminal model = Terminal.builder()
				.emulate(Emulation.VT100)
				.withSize(Size.FIXED_80x24)
				.buildPassive();
		terminal = new TerminalView(model);
		//terminal.setForce9x16(true);
		ScrollPane scroll2 = new ScrollPane(terminal);
		scroll2.setMaxHeight(400);

		console = new GhosttyTerminalView();

        tfInput  = new TextField();
        tfInput.setOnAction(ev -> {
        	sendInput(tfInput.getText());
        	tfInput.clear();
        });
		textLayout = new VBox(10, scroll, scroll2, console.getPane(), tfInput);
		VBox.setVgrow(console.getPane(), Priority.ALWAYS);

		mapView   = new MapView(symbols.getTileGraphicService(), symbols.getSymbolSet("terrain"));

		mapLayout = new VBox(10, mapView);
		mapLayout.setPrefWidth(352);
		mapLayout.setMinHeight(500);

		layout = new HBox(20, textLayout, mapLayout);

		Scene scene = new Scene(layout, 1000,1200);
		stage.setScene(scene);
		stage.setWidth(1000);
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
		
		Config connectWith = mainConfig.getWorld().get("eden");
		connectWith(connectWith);
		
	}

	//-------------------------------------------------------------------
	public void connect(MUDConnection connection) {
		logger.log(Level.INFO, "Connecting to MUD {0}", connection);
		try {
//			((SwitchableOutputStream)console.input()).setSink(connection.getStreamToMUD());
			((SwitchableInputStream)console.output()).setSource(connection.getStreamFromMUD());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static final char ESC = 27;
	private static void writeTerminalCommands(PipedWriter writer) throws IOException {
        writer.write(ESC + "%G");
        writer.write(ESC + "[31m");
        writer.write("Hello\r\n");
        writer.write(ESC + "[32;43m");
        writer.write("World\r\n");
        AParsedElement csi = new DeviceAttributes(Variant.Primary);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(16);
		csi.encode(baos, true);
		byte[] data = baos.toByteArray();
		char[] cata = new char[data.length];
		for (int i=0; i<data.length; i++) {
			cata[i]=(char)(data[i]&0xff);
		}
		writer.write(cata);
        writer.write(ESC + "[0m");
        writer.write("und Welt\r\n");
     }

	private MUDSession startReadingFromMUD(SessionConfig config, Config activeConfig) throws IOException, InterruptedException {
		Charset useCharset = StandardCharsets.UTF_8;
		if (activeConfig.getServerEncoding()!=null)
			useCharset = Charset.forName(activeConfig.getServerEncoding());


		MUDSession session = new MUDSession(config, null, console.getConsoleSize(), useCharset);
		session.getSocket().setOptionListener(TelnetOption.MUSHCLIENT, (AardwolfMushclientListener)this);
//		session.getSocket().setOptionListener(TelnetOption.MSP, sound);
//		session.setGmcpListener(this);

		if ((activeConfig instanceof Config)  &&((Config)activeConfig).getServerEncoding()!=null) {
			console.getInputStream().setEncoding(useCharset);
		}

		logger.log(Level.INFO, "Read from MUD with charset {0}", useCharset);
		ReadFromMUDTask readTask = new ReadFromMUDTask(session.getSocket(), console.getOutputStream(), activeConfig, useCharset);
		readTask.setControlSequenceFilter( frag -> filterFragmentFromMUD(frag));
		session.getStreamToMUD().setLoggingListener( (type,text) -> {if (!"PRINT".equals(type)) logger.log(Level.INFO, "MUD --> {0} = {1}", type,text);});
		Thread readThread = new Thread(readTask);
		readThread.start();

		return session;
	}

	//-------------------------------------------------------------------
	private AParsedElement filterFragmentFromMUD(AParsedElement frag) {
		return frag;
	}

	//-------------------------------------------------------------------
	private ReadFromConsoleTask startReadingFromTerminal(Config activeConfig) throws IOException {
		ReadFromConsoleTask readFromConsole = new ReadFromConsoleTask(console, activeConfig, null);

		Thread readFromTerminal = new Thread(readFromConsole, "FromConsole");
		readFromTerminal.start();
		return readFromConsole;
	}

	//-------------------------------------------------------------------
	/**
	 * @param connectWith
	 */
	private void connectWith(Config connectWith) {
		Thread thread = new Thread(() -> {
			logger.log(Level.INFO, "Now create session");
			try {
	//			session = SessionManager.createSession("rom.mud.de", 4000);
	//			session = SessionManager.createSession("mg.mud.de", 4711);
				session = SessionManager.createSession(connectWith.getServer(), connectWith.getPort());
	//			session = SessionManager.createSession("localhost", 4000);
				InetAddress host = InetAddress.getByName("eden-test.rpgframework.de");
				MUDConnection con = new TCPMUDConnection(host, 4000, session);
				connect(con);
				
//				Thread readFromMUD = new Thread(new InputStreamThread(con.getStreamFromMUD(), console.getOutputStream()));
//				readFromMUD.start();

//				session.connect(new SessionListener() {
//
//					@Override
//					public void textReceived(String msg) {
//						System.out.println("-----\n"+msg);
//						terminal.getTerminal().write(msg);
//						try {
//							console.getOutputStream().write(msg);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
////						try {
//////							terminalWriter.write(msg);
//////							ttyConnector.write(msg);
////						} catch (IOException e) {
////							// TODO Auto-generated catch block
////							e.printStackTrace();
////						}
////				        Platform.runLater( () -> {
////				        	Node pane = FlowBuilder.configure()
////				        			.fontFamily("Monospaced Regular")
////				        			.fontSize(12)
////				        			.darkMode(false)
////				        		.message(msg)
////				        		.build();
////				        	HistoryEntry entry = new HistoryEntry(msg, pane);
////				        	history.add(entry);
////				        	historyPane.getChildren().add(pane);
////				        	if (historyPane.getChildren().size()>20) {
////				        		historyPane.getChildren().remove(0);
////				        		history.remove(0);
////				        	}
////				        	//scroll.setVvalue(1.0);
////				        	});
//					}
//
//					@Override
//					public void connectionLost(Session session) {
//						System.exit(0);
//					}
//
//					@Override
//					public void mapReceived(org.prelle.telnet.mud.MUDTilemapProtocol.TileMapData data) {
//						// TODO Auto-generated method stub
//						mapView.setData(data.getRawData());
//					}
//				});

//				ReadFromConsoleTask readFromConsole = startReadingFromTerminal(connectWith);
//				MUDSession session = startReadingFromMUD(null, connectWith);
//				MUDSession session = MUDSession.builder(console)
//						.setCharset(StandardCharsets.UTF_8)
//						.build();
				//session.co

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		thread.start();

	}

	//-------------------------------------------------------------------
	private void sendInput(String text) {
		session.sendMessage(text);
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

}
