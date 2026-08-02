package org.prelle.mudclient.jfx;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.prelle.ghostty.GhosttyTerminalView;
import org.prelle.jeditermfxterminal.JediTerminalView;
import org.prelle.mudclient.jfx.MUDClientMain.HistoryEntry;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.MUDSessionUserInterface;
import org.prelle.terminal.TerminalEmulator;

import com.graphicmud.symbol.SymbolManager;

import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 
 */
public class MUDSessionUserInterfaceJFX extends VBox implements MUDSessionUserInterface {

	private final static Logger logger = System.getLogger("realmrunner");
	
	private SymbolManager symbols;

	private TabPane sessionTabs;
	private Tab tabPlay;
	private Tab tabSettings;
	private Tab tabMXP;
	
	private ScrollPane scroll;
	private VBox historyPane;
	private TextField tfInput;
	private VBox textLayout;
	private GhosttyTerminalView console;
//	private JediTerminalView console;
	private VBox mapLayout;
	private HBox layout;

	private List<HistoryEntry> history;

	private transient MUDSession session;

	//-------------------------------------------------------------------
	/**
	 */
	public MUDSessionUserInterfaceJFX() {
		getStyleClass().add("mud-session-ui");
		history = new ArrayList<MUDClientMain.HistoryEntry>();
		
		initComponents();
		initLayout();
		initInteractivity();
	}
	
	//-------------------------------------------------------------------
	private void initComponents() {
		historyPane = new VBox();
		historyPane.setSpacing(0);
		historyPane.setMinSize(960,400);

		scroll = new ScrollPane(historyPane);
		scroll.setMaxHeight(400);
		scroll.setMinWidth(960);
		scroll.setFitToWidth(true);

		console = new GhosttyTerminalView();
//		console = new JediTerminalView();
		// console is already on MAX_VALUE
		
//		ScrollPane scroll3 = new ScrollPane(console.getPane());
//		scroll3.setMaxHeight(Double.MAX_VALUE);
//		scroll3.setMaxWidth(Double.MAX_VALUE);
//		scroll3.setFitToHeight(true);
//		scroll3.setFitToWidth(true);
//		//scroll2.setMaxHeight(400);
//
        tfInput  = new TextField();
        tfInput.setPromptText("Your input here...");
        
        
        sessionTabs = new TabPane();
        sessionTabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        sessionTabs.setSide(Side.BOTTOM);
        sessionTabs.setTabMaxHeight(25);
        sessionTabs.getStyleClass().addAll("session","dense");
	}
	
	//-------------------------------------------------------------------
	private void initLayout() {
		// Play tab
		VBox bxPlay = new VBox(console.getPane(),  tfInput);
		VBox.setVgrow(console.getPane(), Priority.ALWAYS);
		tabPlay = new Tab("Play", bxPlay);
		tabPlay.getStyleClass().addAll("session-tab","dense");
		
		// Settings tab
		tabSettings = new Tab("Settings", new Label("TO Do"));
		
		// Settings tab
		TextArea mxpArea = new TextArea();
		mxpArea.setPromptText("No MXP custom tags defined for this session.");
		tabMXP = new Tab("MXP", mxpArea);
		
		sessionTabs.getTabs().addAll(tabPlay, tabSettings);
		sessionTabs.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(sessionTabs, Priority.ALWAYS);
		
		getChildren().addAll(sessionTabs);
//		AnchorPane.setTopAnchor(layout, 0.0);
		
	}
	
	//-------------------------------------------------------------------
	private void initInteractivity() {
		scroll.vvalueProperty().bind(historyPane.heightProperty());
		tfInput.setOnAction(_ -> {
        	logger.log(Level.INFO, "Typed {0}", tfInput.getText());
        	sendInput(tfInput.getText());
        	tfInput.clear();
        });
		
	}

	//-------------------------------------------------------------------
	private void sendInput(String text) {
		console.sendUserInput(text);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDSessionUserInterface#indicateFeatureState(java.lang.String, boolean)
	 */
	@Override
	public void indicateFeatureState(String feature, boolean state) {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDSessionUserInterface#getTerminal()
	 */
	@Override
	public TerminalEmulator getTerminal() {
		return console;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDSessionUserInterface#setSession(org.prelle.realmrunner.network.MUDSession)
	 */
	@Override
	public void setSession(MUDSession value) {
		this.session = value;
		this.session.setSessionListener( sess -> sessionDetailsChanged(sess) );
		
		Optional<String> mxp = value.getMXPDefinitions();
		if (mxp.isPresent()) {
			sessionTabs.getTabs().add(tabMXP);
			TextArea area = (TextArea) tabMXP.getContent();
			area.setText(mxp.get());
		} else {
			sessionTabs.getTabs().remove(tabMXP);
		}
	}

	//-------------------------------------------------------------------
	private void sessionDetailsChanged(MUDSession sess) {
		logger.log(Level.TRACE, "ENTER: sessionDetailsChanged");
		Optional<String> mxp = sess.getMXPDefinitions();
		if (mxp.isPresent()) {
			sessionTabs.getTabs().add(tabMXP);
			TextArea area = (TextArea) tabMXP.getContent();
			area.setText(mxp.get());
		} else {
			sessionTabs.getTabs().remove(tabMXP);
		}
		logger.log(Level.TRACE, "LEAVE: sessionDetailsChanged");
	}

}
