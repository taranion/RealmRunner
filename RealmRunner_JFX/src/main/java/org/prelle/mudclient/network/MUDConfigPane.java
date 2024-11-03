package org.prelle.mudclient.network;

import org.prelle.javafx.TitledComponent;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * 
 */
public class MUDConfigPane extends VBox {
	
	private TextField tfName;
	private TextField tfHost;
	private TextField tfPort;
	private TextField tfLogin;
	private PasswordField tfPasswd;

	private GridPane grid;
	
	//-------------------------------------------------------------------
	public MUDConfigPane() {
		initComponents();
		initLayout();
	}

	//-------------------------------------------------------------------
	private void initComponents() {
		tfName = new TextField();
		tfHost = new TextField();
		tfPort = new TextField();
		tfLogin = new TextField();
		tfPasswd = new PasswordField();
	}

	//-------------------------------------------------------------------
	private void initLayout() {
		TitledComponent tcName = new TitledComponent("Profile name", tfName);
		TitledComponent tcHost = new TitledComponent("Host", tfHost);
		TitledComponent tcPort = new TitledComponent("Port", tfPort);
		TitledComponent tcLogin  = new TitledComponent("Login", tfLogin);
		TitledComponent tcPasswd = new TitledComponent("Password", tfPasswd);
		
		tcName.setTitleMinWidth(100.0);
		tcHost.setTitleMinWidth(100.0);
		tcPort.setTitleMinWidth(100.0);
		tcLogin.setTitleMinWidth(100.0);
		tcPasswd.setTitleMinWidth(100.0);
		
		grid = new GridPane(10, 10);
		grid.add(tcName, 0, 0);
		grid.add(tcHost, 0, 1);
		grid.add(tcPort, 0, 2);
		grid.add(tcLogin, 0, 3);
		grid.add(tcPasswd, 0, 4);
		
		getChildren().add(grid);
	}

	//-------------------------------------------------------------------
	public void setData(String name, Config config) {
		tfName.setText(name);
		tfHost.setText(config.getServer());
		tfPort.setText(String.valueOf(config.getPort()));
		tfLogin.setText(config.getLogin());
		tfPasswd.setText(config.getPassword());
	}

}
