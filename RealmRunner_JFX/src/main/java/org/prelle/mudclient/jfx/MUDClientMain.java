package org.prelle.mudclient.jfx;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.prelle.fxterminal.TerminalView;
import org.prelle.mud.symbol.DefaultSymbolManager;
import org.prelle.mud.symbol.SymbolManager;
import org.prelle.mud.symbol.jfx.JavaFXTileGraphicLoader;
import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.DataFileManager;
import org.prelle.realmrunner.network.MainConfig;
import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.Terminal.Size;
import org.prelle.terminal.emulated.delete.Emulation;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
//				new SwingTileGraphicLoader(Paths.get("/home/prelle/git/MUD2024/Example MUD","src/main/resources/static/symbols")));
				new JavaFXTileGraphicLoader(Paths.get("/home/prelle/git/MUD2024/Example MUD","src/main/resources/static/symbols")));
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
		scroll.setMinHeight(400);
		scroll.setMinWidth(960);
		scroll.setFitToWidth(true);
		scroll.vvalueProperty().bind(historyPane.heightProperty());

		Terminal model = Terminal.builder()
				.emulate(Emulation.VT100)
				.withSize(Size.FIXED_80x24)
				.buildPassive();
		terminal = new TerminalView(model);
		//terminal.setForce9x16(true);


        tfInput  = new TextField();
        tfInput.setOnAction(ev -> {
        	sendInput(tfInput.getText());
        	tfInput.clear();
        });
		textLayout = new VBox(10, scroll, terminal, tfInput);

		mapView   = new MapView(symbols.getTileGraphicService(), symbols.getSymbolSet(2));

		mapLayout = new VBox(10, mapView);
		mapLayout.setPrefWidth(352);
		mapLayout.setMinHeight(500);

		layout = new HBox(20, textLayout, mapLayout);

		Scene scene = new Scene(layout, 1000,1000);
		stage.setScene(scene);
		stage.setWidth(1000);
		stage.show();
		
		Stage dialogStage = new Stage();
		ConnectionDialog choices = new ConnectionDialog(mainConfig);
		Scene dialogScene = new Scene(choices);
		dialogStage.setScene(dialogScene);
		dialogStage.showAndWait();
		
		Config connectWith = choices.getSelected();
		logger.log(Level.DEBUG, "Connect to {0}", connectWith);
		if (connectWith!=null) {
		
		}
	}

	private void connectWith(Config connectWith) {
		Thread thread = new Thread(() -> {
			logger.log(Level.INFO, "Now create session");
			try {
	//			session = SessionManager.createSession("rom.mud.de", 4000);
	//			session = SessionManager.createSession("mg.mud.de", 4711);
				session = SessionManager.createSession(connectWith.getServer(), connectWith.getPort());
	//			session = SessionManager.createSession("localhost", 4000);
				session.connect(new SessionListener() {

					@Override
					public void textReceived(String msg) {
						System.out.println("-----\n"+msg);
						terminal.getTerminal().write(msg);
				        Platform.runLater( () -> {
				        	Node pane = FlowBuilder.configure()
				        			.fontFamily("Monospaced Regular")
				        			.fontSize(12)
				        			.darkMode(false)
				        		.message(msg)
				        		.build();
				        	HistoryEntry entry = new HistoryEntry(msg, pane);
				        	history.add(entry);
				        	historyPane.getChildren().add(pane);
				        	if (historyPane.getChildren().size()>20) {
				        		historyPane.getChildren().remove(0);
				        		history.remove(0);
				        	}
				        	//scroll.setVvalue(1.0);
				        	});
					}

					@Override
					public void connectionLost(Session session) {
						System.exit(0);
					}

					@Override
					public void mapReceived(org.prelle.telnet.mud.MUDTilemapProtocol.TileMapData data) {
						// TODO Auto-generated method stub
						mapView.setData(data.getRawData());
					}
				});
			} catch (IOException e) {
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
		String homeDir = System.getProperty("user.home", "/tmp");
		Path configFile = Paths.get(homeDir, ".realmrunner.yml");
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
