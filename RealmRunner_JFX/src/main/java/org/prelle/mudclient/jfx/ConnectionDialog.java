package org.prelle.mudclient.jfx;

import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.MainConfig;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 
 */
public class ConnectionDialog extends HBox {
	
	private MainConfig config;
	
	private ListView<String> lvWorlds;
	private MUDConfigPane bxDetail;
	
	private Button btnConnect;
	private Button btnCancel;
	
	private Config selected;

	//-------------------------------------------------------------------
	public ConnectionDialog(MainConfig config) {
		initComponents();
		initLayout();
		initInteractivity();
		setData(config);
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		lvWorlds = new ListView<String>();
		bxDetail = new MUDConfigPane();
		
		btnConnect = new Button("Connect");
		btnConnect.setDefaultButton(true);
		btnCancel = new Button("Cancel");
		btnCancel.setCancelButton(true);
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		super.setSpacing(20);
		super.setFillHeight(true);
		bxDetail.setMaxHeight(Double.MAX_VALUE);
		
		ButtonBar buttons = new ButtonBar();
		buttons.getButtons().addAll(btnCancel, btnConnect);
		
		Region fill = new Region();
		fill.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(fill, Priority.ALWAYS);

		VBox detailWithButtons = new VBox(10,bxDetail, fill,buttons);
		detailWithButtons.setMaxHeight(Double.MAX_VALUE);
		VBox.setMargin(bxDetail, new Insets(10));
		VBox.setMargin(buttons, new Insets(10));

		getChildren().addAll(lvWorlds, detailWithButtons);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		lvWorlds.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			if (n!=null) {
				Config cfg = config.getWorld().getOrDefault(n, new Config());
				bxDetail.setData(n, cfg);
				selected = cfg;
			}
		});
		
		btnCancel.setOnAction(ev -> {selected=null; this.getScene().getWindow().hide();});
		btnConnect.setOnAction(ev -> {this.getScene().getWindow().hide();});
	}
	
	//-------------------------------------------------------------------
	public void setData(MainConfig config) {
		this.config = config;
		lvWorlds.getItems().setAll(config.getWorlds().stream().map(entry -> entry.getKey()).toList());
	}

	//-------------------------------------------------------------------
	/**
	 * @return the selected
	 */
	public Config getSelected() {
		return selected;
	}
	
}
