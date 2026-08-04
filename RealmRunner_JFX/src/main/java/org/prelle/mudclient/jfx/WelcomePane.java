package org.prelle.mudclient.jfx;

import java.util.Map.Entry;
import java.util.function.Consumer;

import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.MainConfig;
import org.prelle.realmrunner.network.SessionProtocol;

import com.graphicmud.Localization;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.Tile;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/**
 * Hero-Banner with last MUD played
 * Area to enter new connection
 * Catalog of known MUDs
 */
public class WelcomePane extends VBox {

	private Consumer<Config> onWorldSelected;
	private TilePane tpRecent;
	private TilePane tpAll;

	//-------------------------------------------------------------------
	public WelcomePane(Consumer<Config> onWorldSelected) {
		super(20);
		this.getStyleClass().add("welcome-pane");
		this.onWorldSelected = onWorldSelected;
		
		initLayout();
		initializeRecent();
		initializeNewConnection();
		initializeWorlds();
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		Label lblTitle = new Label("RealmRunner");
		lblTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
		
		getChildren().addAll(lblTitle);
	}

	//-------------------------------------------------------------------
	private void initializeRecent() {
		Label lbWorlds = new Label(Localization.getString("heading.recent"));
		lbWorlds.getStyleClass().addAll(Styles.TITLE_3);
		tpRecent = new TilePane(Orientation.HORIZONTAL, 10,10);
//		tpRecent.setPrefTileHeight(60);
		
		VBox worldBox = new VBox(10, lbWorlds, tpRecent);
		getChildren().addAll(worldBox);
	}

	//-------------------------------------------------------------------
	private void initializeNewConnection() {
		Label lbWorlds = new Label(Localization.getString("heading.new"));
		lbWorlds.getStyleClass().addAll(Styles.TITLE_3);
//		tpRecent = new TilePane(Orientation.HORIZONTAL, 10,10);
		
		var cbProto = new ComboBox<SessionProtocol>();
		cbProto.getItems().addAll(SessionProtocol.values());
		cbProto.getSelectionModel().selectFirst();

		var tfHost = new TextField();
		tfHost.setPromptText("server");
		tfHost.setAccessibleText("Server Name or Address");
		HBox.setHgrow(tfHost, Priority.ALWAYS);

		var tfPort = new TextField();
		tfPort.setPromptText("4000");
		tfPort.setPrefColumnCount(4);
		tfPort.setFocusTraversable(true);
		tfPort.setAccessibleText("Port number");
		tfPort.setAccessibleRoleDescription("Eingabefeld für die Portnummer");
		HBox.setHgrow(tfHost, Priority.ALWAYS);
		
		var lblPort = new Label("Port number");
		lblPort.setLabelFor(tfPort);

		// Falls das Label auf der UI nicht sichtbar sein soll:
		lblPort.setVisible(false);
		lblPort.setManaged(false);
		
		tfPort.disableProperty().bind(cbProto.valueProperty().isEqualTo(SessionProtocol.WEBSOCKET.name()));

		var group = new InputGroup(cbProto, tfHost, tfPort);
		group.setMaxWidth(400);
		
		VBox worldBox = new VBox(10, lbWorlds, group);
		getChildren().addAll(worldBox);
	}

	//-------------------------------------------------------------------
	private void initializeWorlds() {
		Label lbWorlds = new Label(Localization.getString("heading.worlds"));
		lbWorlds.getStyleClass().addAll(Styles.TITLE_3);
		tpAll = new TilePane(Orientation.HORIZONTAL, 10,10);
		
		VBox worldBox = new VBox(10, lbWorlds, tpAll);
		getChildren().addAll(worldBox);
	}

	//-------------------------------------------------------------------
	public void setData(MainConfig config) {
		tpRecent.getChildren().clear();
		for (Entry<String,Config> recent : config.getWorlds()) {
			Node tile = getTileFor(recent.getKey(), recent.getValue());
			tpRecent.getChildren().add(tile);
		}
	}

	//-------------------------------------------------------------------
	private Node getTileFor(String name, Config config) {
		System.out.println("Creating tile for "+name+" "+config);
		Label lbName = new Label(name);
		Label lbHost = new Label("telnet://"+config.getServer() + ":" + config.getPort());
		VBox bxData = new VBox(5, lbName, new Separator(),lbHost);
		
//		var card1 = new Card();
//		card1.getStyleClass().add(Styles.DENSE);
//		card1.setMinWidth(250);
//		card1.setMaxWidth(250);
//		card1.setHeader(new Tile(
//		    name,
//		    config.getServer()
//		));
//		card1.setBody(new Label("This is content"));
//		card1.setStyle("-fx-background-color: #ffffff; -fx-padding: 0px;");
//		
//		card1.setOnMouseEntered( _ -> card1.getStyleClass().add(Styles.ELEVATED_2));
//		card1.setOnMouseExited( _ -> card1.getStyleClass().remove(Styles.ELEVATED_2));
//		card1.setOnMouseClicked( _ -> onWorldSelected.accept(config));

		Tile tile = new Tile(
			    name,
			    config.getServer()
			);
		tile.setOnMouseEntered( _ -> tile.setStyle("-fx-background-color: #e0e0e0;"));
		tile.setOnMouseExited( _ -> tile.setStyle("-fx-background-color: transparent;"));
		//tile.setOnMouseClicked( _ -> onWorldSelected.accept(config));
//		Button tile = new Button(null,bxData);
//		tile.setMaxWidth(Double.MAX_VALUE);
//		
		tile.setActionHandler( () -> onWorldSelected.accept(config));
		
		return tile;
	}
}
