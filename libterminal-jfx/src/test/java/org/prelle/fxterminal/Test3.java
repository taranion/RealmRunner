package org.prelle.fxterminal;

import org.prelle.terminal.emulated.Emulation;
import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.Terminal.Size;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 *
 */
public class Test3 extends Application {

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
		Terminal terminal = Terminal.builder()
			.emulate(Emulation.VT100)
			.withSize(Size.FIXED_80x24)
			.buildPassive();

		TerminalView view = new TerminalView(terminal);
		terminal.setView(view);
		System.out.println("x");
		view.setForce9x16(false);
		System.out.println("y");
//		view.impl_setFont(Font.font("Monospace", 12));
		view.impl_setFont(Font.loadFont(ClassLoader.getSystemResourceAsStream("AcPlus_IBM_VGA_9x16-2x.ttf"), 16));
		System.out.println("z");

		Scene scene = new Scene(view,800,800);
		primaryStage.setScene(scene);
		primaryStage.show();
		System.out.println("Showing");

//		terminal.write("12345678901234567890123456789012345678901234567890123456789012345678901234567890         1         2         3         4         5         6         7         8Lorem \\033[H\\033[2Jipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.".getBytes());
		terminal.write("12345678901234567890123456789012345678901234567890123456789012345678901234567890         1         2         3         4         5         6         7         8Lorem \\033[H\\033[2Jipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.");
//		terminal.getOutputStream().flush();
//		String screen = new String(Files.readAllBytes(Paths.get("screen2_unicode.ans")), Charset.forName("UTF-8"));
////		terminal.getOutputStream().write(screen.getBytes());
////		terminal.getOutputStream().flush();
//		terminal.write(screen);
//		terminal.write("[0m");
//		terminal.write("12345678901234567890123456789012345678901234567890123456789012345678901234567890\n");
//		terminal.write("         1         2         3         4         5         6         7         8\n");
//		terminal.write("Hallo Welt\nWie geht es Dir?");

//		((FXTerminalSkin)view.getSkin()).drawGrid();
	}

}
