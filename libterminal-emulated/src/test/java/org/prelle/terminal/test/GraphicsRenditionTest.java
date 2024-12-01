package org.prelle.terminal.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.prelle.terminal.emulated.delete.ColorPalette;

public class GraphicsRenditionTest {

	private final static char ESC = (char)27;

		//-------------------------------------------------------------------
		public static String CSI(int n, int...param) {
			StringBuffer buf = new StringBuffer( ((char)0x1B)+"["+n);
			for (int p : param)
				buf.append(";"+p);
			return buf+"m";
		}

		//-------------------------------------------------------------------
		public static String BG(int color256) {
			return CSI(48,5,color256);
		}

		//-------------------------------------------------------------------
		public static String FG(int color256) {
			return CSI(38,5,color256);
		}

		//-------------------------------------------------------------------
		public static String reset() {
			return CSI(0);
		}
	//-------------------------------------------------------------------
	public static String getVT100GRTest() {
		StringBuffer buf = new StringBuffer();
		buf.append(String.format("%20sGraphic rendition test pattern:\n\n", ""));
		buf.append(String.format("%-36s    "+CSI(1)+"%-36s\n", "vanilla", "bold"+CSI(0)+reset()+"\n"));
		buf.append(String.format("     "+CSI(4)+"underline"+CSI(0)+"%22s         "+CSI(4)+CSI(1)+"bold underline"+CSI(0)+"\n\n", ""));
		buf.append(String.format(CSI(5)+"blink"+CSI(0)+"%-31s    "+CSI(5)+CSI(1)+"bold blink"+CSI(0)+"%-26s"+reset()+"\n\n", "", ""));
		buf.append(String.format("     "+CSI(4)+CSI(5)+"underline blink"+CSI(0)+"%16s         "+CSI(4)+CSI(1)+CSI(5)+"bold underline blink"+CSI(0)+"\n\n", ""));
		buf.append(String.format(CSI(7)+"negative"+CSI(0)+"%-28s    "+CSI(7)+CSI(1)+"bold negative"+CSI(0)+"%-23s"+reset()+"\n\n", "", ""));
		buf.append(String.format("     "+CSI(4)+CSI(7)+"underline negative"+CSI(0)+"%14s         "+CSI(4)+CSI(7)+CSI(1)+"bold underline negative"+CSI(0)+"\n\n", ""));
		buf.append(String.format(CSI(7)+CSI(5)+"blink negative"+CSI(0)+"%-22s    "+CSI(7)+CSI(5)+CSI(1)+"bold blink negative"+CSI(0)+"%-17s"+reset()+"\n\n", "", ""));
		buf.append(String.format("     "+CSI(4)+CSI(5)+CSI(7)+"underline blink negative"+CSI(0)+"%8s         "+CSI(4)+CSI(5)+CSI(7)+CSI(1)+"bold underline blink negative"+CSI(0)+"\n\n", ""));

		return buf.toString();
	}

	//-------------------------------------------------------------------
	public static String COL(int color) {
		int fore = ColorPalette.WHITE;
		if (color>=8  & color<16) fore=ColorPalette.BLACK;

		return BG(color)+FG(fore)+String.format("%-3s", color);
	}

	//-------------------------------------------------------------------
	public static String get256ColorTest() {
		StringBuffer buf = new StringBuffer();
		buf.append("Standard: "+FG(ColorPalette.BRIGHT_WHITE));
		for (int i=0; i<8; i++) buf.append(BG(i)+String.format("%4s ",i)); buf.append(reset()+"\n");
		buf.append("Intense:  "+FG(ColorPalette.BLACK));
		for (int i=8; i<16; i++) buf.append(BG(i)+String.format("%4s ",i)); buf.append(reset()+"\n");
		buf.append("\n");
		for (int line=0; line<18; line++) {
			for (int col=0; col<12; col++) {
				if (col==0) buf.append(FG(ColorPalette.BRIGHT_WHITE));
				if (col==6) buf.append(FG(ColorPalette.BLACK));
				int square = (line/6) + (col/6)*3;
				int color  = 16 + 6*square + (col%6) + (line%6)*36;
//				System.out.println(square + "/"+color);
				buf.append(BG(color)+String.format("%4s ",color));
				if (col==5)
					buf.append(reset()+"   ");
			}
			buf.append(reset()+"\n");
			if (line==5) buf.append("\n");
			if (line==11) buf.append("\n");
			if (line==17) buf.append("\n");
		}
		buf.append("Grays:  "+FG(ColorPalette.BRIGHT_WHITE));
		for (int i=232; i<244; i++) buf.append(BG(i)+String.format("%4s ",i)); buf.append(reset()+"\n");
		buf.append("        "+FG(ColorPalette.BLACK));
		for (int i=244; i<256; i++) buf.append(BG(i)+String.format("%4s ",i)); buf.append(reset()+"\n");

		return buf.toString();
	}

	//-------------------------------------------------------------------
	public static void main(String[] args) throws IOException {
//		System.out.println(getVT100GRTest());

		File tmp = new File("/tmp/sgr-test.ans");
		FileWriter out = new FileWriter(tmp);
		out.append(getVT100GRTest());
		out.flush();
		out.close();

		System.out.println(get256ColorTest());
		tmp = new File("/tmp/256color.ans");
		out = new FileWriter(tmp);
		out.append(get256ColorTest());
		out.flush();
		out.close();
//		System.console().writer().append(getVT100GRTest());
	}

}
