package org.prelle.terminal.emulated.delete;

import org.prelle.terminal.emulated.delete.Style.RGB;

/**
 *
 */
public class ColorPalette {

	public final static int BLACK  = 0;
	public final static int RED    = 1;
	public final static int GREEN  = 2;
	public final static int YELLOW = 3;
	public final static int BLUE   = 4;
	public final static int MAGENTA= 5;
	public final static int CYAN   = 6;
	public final static int WHITE  = 7;
	public final static int BRIGHT_BLACK  = 8;
	public final static int BRIGHT_RED    = 9;
	public final static int BRIGHT_GREEN  = 10;
	public final static int BRIGHT_YELLOW = 11;
	public final static int BRIGHT_BLUE   = 12;
	public final static int BRIGHT_MAGENTA= 13;
	public final static int BRIGHT_CYAN   = 14;
	public final static int BRIGHT_WHITE  = 15;

	public static RGB TRANSPARENT = new RGB(-1,-1,-1);

	public final static RGB[] VGA = new RGB[] {
			new RGB(  0,  0,  0),
			new RGB(170,  0,  0),
			new RGB(  0,170,  0),
			new RGB(170, 85,  0),
			new RGB(  0,  0,170),
			new RGB(170,  0,170),
			new RGB(  0,170,170),
			new RGB(170,170,170),
			new RGB( 85, 85, 85),
			new RGB(255, 85, 85),
			new RGB( 85,255, 85),
			new RGB(255,255, 85),
			new RGB( 85, 85,255),
			new RGB(255, 85,255),
			new RGB( 85,255,255),
			new RGB(255,255,255)
	};

	public final static RGB[] ANSI = new RGB[256];

	static {
		int[] intensity = new int[] {0,95,135,175,215,255};
		for (int x=0; x<intensity.length; x++) {
			for (int y=0; y<intensity.length; y++) {
				for (int z=0; z<intensity.length; z++) {
					String hex = "#"+intensity[x]+intensity[y]+intensity[z];
					int code = 16+x*36 + y*6 + z;
					ANSI[code] = new RGB(intensity[x],intensity[y],intensity[z]);
				}

			}

		}
		for (int i=232; i<256; i++) {
			int x = 8+(i-232)*10;
			ANSI[i] = new RGB(x,x,x);
		}
	}

	//-------------------------------------------------------------------
	/**
	 */
	public ColorPalette() {
		// TODO Auto-generated constructor stub
	}

}
