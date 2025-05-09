package org.prelle.mudansi;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.DeviceAttributes.OperatingLevel;
import org.prelle.ansi.commands.SetConformanceLevel;
import org.prelle.ansi.control.AreaControls;
import org.prelle.ansi.control.CursorControls;

/**
 *
 */
public class TopBottomSplit implements UserInterfaceFormat {

	private final static Logger logger = System.getLogger(TopBottomSplit.class.getPackageName());

	protected int columns;
	protected int rows;

	private boolean withInputBuffer;

//	//-------------------------------------------------------------------
//	/**
//	 * @param args
//	 * @throws IOException
//	 */
//	public static void main(String[] args) throws IOException {
//		TerminalEmulator console;
//		if (Platform.isWindows()) {
//			console = new WindowsConsole();
//		} else {
//			console = new UnixConsole();
//		}
//		console.setMode(TerminalMode.RAW);
//		console.setLocalEchoActive(false);
//
//		ANSIOutputStream out = console.getOutputStream();
//		out.setLoggingListener( (c,msg) -> System.out.println("--> "+c+" = "+msg));
//		ANSIInputStream in = console.getInputStream();
//		in.setLoggingListener( (c,msg) -> System.out.println("<-- "+c+" = "+msg));
//
//		CapabilityDetector detector = new CapabilityDetector(out);
//
//		Thread inputReader = new Thread( () -> {
//			try {
//				logger.log(Level.INFO, "---------------Start reading------------");
//				while (true) {
//					AParsedElement frag =  in.readFragment();
//					detector.process(frag);
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}, "reader");
//		inputReader.start();
//		detector.performCheck();
//
////		new TopBottomSplit(out);
////		Instant start = Instant.now();
////		do {
////			AParsedElement frag = in.readFragment();
////			System.out.println("READ "+frag);
////		} while (Duration.between(start, Instant.now()).toSeconds()<20);
//		System.exit(0);
//	}

	//-------------------------------------------------------------------
	public TopBottomSplit(int width, int height) {
		if (width==0)
			throw new IllegalArgumentException();
		this.columns = width;
		this.rows= height;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudansi.UserInterfaceFormat#initialize(org.prelle.ansi.ANSIOutputStream)
	 */
	@Override
	public void initialize(ANSIOutputStream out) throws IOException {
		logger.log(Level.INFO, "ENTER: initialize");
		AreaControls.clearScreen(out);

		logger.log(Level.DEBUG, "ENTER: initializeHorizontalInterface");
		logger.log(Level.DEBUG, "Screen size is {0}x{1}", columns, rows);

		// Draw upper border for map and description
		int mapWidth=11;
		int mapHeight=11;
		StringBuffer line1 = new StringBuffer("\u2554");
		line1.repeat("\u2550", mapWidth);
		line1.append('\u2566');
		line1.repeat("\u2550", columns-mapWidth-3);
		line1.append('\u2557');
		StringBuffer midline = new StringBuffer("\u2551");
		midline.repeat(" ", mapWidth);
		midline.append('\u2551');
		midline.repeat(" ", columns-mapWidth-3);
		midline.append('\u2551');
		StringBuffer line2 = new StringBuffer("\u255A");
		line2.repeat("\u2550", mapWidth);
		line2.append('\u2569');
		line2.repeat("\u2550", columns-mapWidth-3);
		line2.append('\u255D');

		try {
			logger.log(Level.DEBUG, "Set cursor to 1,1");
			CursorControls.setCursorPosition(out, 1, 1);
			out.flush();
			out.write(line1.toString()+"\r\n");
			for (int i=0; i<mapHeight; i++)
				out.write(midline.toString()+"\r\n");
			out.write(line2.toString());
			logger.log(Level.DEBUG, "Set margins");
			AreaControls.setTopAndBottomMargins(out, mapHeight+3, rows);
			logger.log(Level.DEBUG, "Set cursor to 1,x");
			CursorControls.setCursorPosition(out, 1, mapHeight+3);

			logger.log(Level.DEBUG, "Set conformance");
			out.write(new SetConformanceLevel(OperatingLevel.LEVEL4_VT420c));
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			logger.log(Level.DEBUG, "LEAVE: initializeHorizontalInterface");
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudansi.UserInterfaceFormat#sendRoomDescription(org.prelle.ansi.ANSIOutputStream, java.util.List)
	 */
	@Override
	public void sendRoomDescription(ANSIOutputStream out, List<String> lines) throws IOException {
		int mapWidth=11;
		int mapHeight=11;
		CursorControls.savePositionDEC(out);
		//AreaControls.
		int y=0;
		for (String line : lines) {
			if (y>=mapHeight)
				break;
			CursorControls.setCursorPosition(out, mapWidth+3, 2+y);
			out.write(line);
			y++;
		}

		CursorControls.restorePositionDEC(out);
	}
}
