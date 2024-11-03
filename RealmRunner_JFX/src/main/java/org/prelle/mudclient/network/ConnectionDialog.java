package org.prelle.mudclient.network;

import java.util.Map.Entry;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * 
 */
public class ConnectionDialog extends HBox {
	
	private MainConfig config;
	
	private ListView<String> lvWorlds;
	private MUDConfigPane bxDetail;

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
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		super.setSpacing(20);
		getChildren().addAll(lvWorlds, bxDetail);
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		lvWorlds.getSelectionModel().selectedItemProperty().addListener( (ov,o,n) -> {
			if (n!=null) {
				Config cfg = config.getWorld().getOrDefault(n, new Config());
				bxDetail.setData(n, cfg);
			}
		});
	}
	
	//-------------------------------------------------------------------
	public void setData(MainConfig config) {
		this.config = config;
		lvWorlds.getItems().setAll(config.getWorlds().stream().map(entry -> entry.getKey()).toList());
	}
	
}
