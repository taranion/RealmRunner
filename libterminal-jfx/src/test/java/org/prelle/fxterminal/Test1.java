package org.prelle.fxterminal;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.Terminal.Size;
import org.prelle.terminal.emulated.delete.Emulation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 *
 */
public class Test1 extends Application {

	//-------------------------------------------------------------------
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		launch(args);
	}
//
//	private ObservableList<String> getMonoFontFamilyNames() {
//
//	    // Compare the layout widths of two strings. One string is composed
//	    // of "thin" characters, the other of "wide" characters. In mono-spaced
//	    // fonts the widths should be the same.
//
//	    final Text thinTxt = new Text("1 l"); // note the space
//	    final Text thikTxt = new Text("MWX");
//
//	    List<String> fontFamilyList = Font.getFamilies();
//	    List<String> monoFamilyList = new ArrayList<>();
//
//	    Font font;
//
//	    for (String fontFamilyName : fontFamilyList) {
//	        font = Font.font(fontFamilyName, FontWeight.NORMAL, FontPosture.REGULAR, 14.0d);
//	        thinTxt.setFont(font);
//	        thikTxt.setFont(font);
//	        if (thinTxt.getLayoutBounds().getWidth() == thikTxt.getLayoutBounds().getWidth()) {
//	            monoFamilyList.add(fontFamilyName);
//	        }
//	    }
//
//	    return FXCollections.observableArrayList(monoFamilyList);
//	}


	//-------------------------------------------------------------------
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		Terminal model = Terminal.builder()
				.emulate(Emulation.VT100)
				.withSize(Size.FIXED_80x24)
				.buildPassive();
		TerminalView terminal = new TerminalView(model);
		model.setView(terminal);
		terminal.setForce9x16(true);
//		terminal.setFont(Font.font("Monospace", 12));
		terminal.impl_setFont(Font.loadFont(ClassLoader.getSystemResourceAsStream("AcPlus_IBM_VGA_9x16-2x.ttf"), 12));
//		terminal.impl_setFont(Font.loadFont(new FileInputStream("/usr/share/fonts/google-noto-vf/NotoSansMono[wght].ttf"), 16));
		Scene scene = new Scene(terminal,800,800);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Test1");
		primaryStage.show();
		System.out.println("Showing");

//		terminal.getOutputStream().write("12345678901234567890123456789012345678901234567890123456789012345678901234567890         1         2         3         4         5         6         7         8Lorem \\033[H\\033[2Jipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.".getBytes());
//		terminal.getOutputStream().flush();
		String screen = new String(Files.readAllBytes(Paths.get("screen2_unicode.ans")), Charset.forName("UTF-8"));
//		String screen = new String(Files.readAllBytes(Paths.get("/tmp/US-HAZ2.ANS")), Charset.forName("UTF-8"));
//		terminal.getOutputStream().write(screen.getBytes());
//		terminal.getOutputStream().flush();
		model.write(screen);
		//model.write("\u2584");
//		model.write("[0m");
//		model.write("12345678901234567890123456789012345678901234567890123456789012345678901234567890\n");
//		model.write("         1         2         3         4         5         6         7         8\n");
//		model.write("Hallo Welt\nWie geht es Dir?");

//		((FXTerminalSkin)terminal.getSkin()).drawGrid();
	}

}
