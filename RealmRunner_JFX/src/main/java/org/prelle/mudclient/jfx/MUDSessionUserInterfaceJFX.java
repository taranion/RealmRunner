package org.prelle.mudclient.jfx;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.prelle.jeditermfxterminal.GhosttyTerminalView;
import org.prelle.jeditermfxterminal.JediTerminalView;
import org.prelle.mudclient.jfx.MUDClientMain.HistoryEntry;
import org.prelle.realmrunner.network.DataFileManager;
import org.prelle.realmrunner.network.MUDSessionUserInterface;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.Terminal.Size;
import org.prelle.terminal.emulated.delete.Emulation;

import com.graphicmud.symbol.DefaultSymbolManager;
import com.graphicmud.symbol.SymbolManager;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 
 */
public class MUDSessionUserInterfaceJFX extends AnchorPane implements MUDSessionUserInterface {

	private final static Logger logger = System.getLogger("realmrunner");

	private SymbolManager symbols;

	private ScrollPane scroll;
	private VBox historyPane;
	private TextField tfInput;
	private VBox textLayout;
	private GhosttyTerminalView console;
//	private JediTerminalView console;
	private VBox mapLayout;
	private HBox layout;

	private List<HistoryEntry> history;

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
		//console = new JediTerminalView();
		((Pane)console.getPane()).setPrefWidth(1000);
		((Pane)console.getPane()).setPrefHeight(800);
		((Pane)console.getPane()).setMaxWidth(Double.MAX_VALUE);
		((Pane)console.getPane()).setMaxHeight(Double.MAX_VALUE);
		ScrollPane scroll3 = new ScrollPane(console.getPane());
		scroll3.setMaxHeight(Double.MAX_VALUE);
		scroll3.setMaxWidth(Double.MAX_VALUE);
		scroll3.setFitToHeight(true);
		scroll3.setFitToWidth(true);
		//scroll2.setMaxHeight(400);

        tfInput  = new TextField();
		textLayout = new VBox(10, scroll3, tfInput);
		VBox.setVgrow(scroll3, Priority.ALWAYS);


	}
	
	//-------------------------------------------------------------------
	private void initLayout() {
		layout = new HBox(20, textLayout);
		HBox.setHgrow(textLayout, Priority.ALWAYS);
		
		super.getChildren().add(layout);
		AnchorPane.setTopAnchor(layout, 0.0);
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

}
