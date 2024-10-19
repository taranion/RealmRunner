package org.prelle.fxterminal;

import org.prelle.fxterminal.impl.FXTerminalSkin;
import org.prelle.terminal.emulated.Emulation;
import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.Terminal.Size;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 *
 */
public class Test2 extends Application {

	//-------------------------------------------------------------------
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}

	//-------------------------------------------------------------------
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		String content = "iiAA   ▄▄\u2580\u2591\u2592\u2593\u2581\u2594\u250C\u2500\u2500\u2510\r\nXXqj\u2713\u2B12\u2BC0\u2588";

		Font font = Font.font("Monospace", 12);
		font = Font.loadFont(ClassLoader.getSystemResourceAsStream("AcPlus_IBM_VGA_9x16-2x.ttf"), 12);
		TextArea textArea = new TextArea(content);
		textArea.setStyle("-fx-padding: 0px");
		textArea.setPadding(new Insets(0));
		textArea.setPrefHeight(2);
		textArea.setFont(font);

		Text text = new Text(content);
		text.setFont(font);
		TextFlow textFlow = new TextFlow(text);
		textFlow.setLineSpacing(0);

		Terminal model = Terminal.builder()
				.emulate(Emulation.VT100)
				.withSize(Size.FIXED_80x24)
				.buildPassive();
		TerminalView terminal = new TerminalView(model);
		model.setView(terminal);
		terminal.setForegroundColor(Color.LIGHTGRAY);
		terminal.setForce9x16(true);
		terminal.setPrefSize(600, 100);

		Scene scene = new Scene(new VBox(textArea, textFlow, terminal));
		primaryStage.setScene(scene);
		primaryStage.show();

		terminal.getTerminal().write(content);
//		terminal.getWriter().flush();
		((FXTerminalSkin)terminal.getSkin()).drawGrid();
	}

}
