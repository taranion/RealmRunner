package org.prelle.terminal.emulated.delete;

/**
 *
 */
public class Style {

	public static Style DEFAULT = new Style(RGB.WHITE, RGB.TRANSPARENT);

	public static record RGB(int r, int g, int b) {
		public static RGB TRANSPARENT = new RGB(-1,-1,-1);
		public static RGB BLACK  = new RGB(  0,  0,  0);
		public static RGB RED    = new RGB(170,  0,  0);
		public static RGB GREEN  = new RGB(  0,170,  0);
		public static RGB YELLOW = new RGB(170, 85,  0);
		public static RGB BLUE   = new RGB(  0,  0,170);
		public static RGB MAGENTA= new RGB(170,  0,170);
		public static RGB CYAN   = new RGB(  0,170,170);
		public static RGB WHITE  = new RGB(170,170,170);
		public static RGB[] ANSI_COLORS = new RGB[] {BLACK,RED,GREEN,YELLOW,BLUE,MAGENTA,CYAN};
	}

	public static enum FontWeight {
		REGULAR,
		BOLD,
		LIGHT
	}
	public static enum Blink {
		NONE,
		SLOW,
		RAPID
	}

	public RGB foreground;
	public RGB background;
	public FontWeight weight = FontWeight.REGULAR;
	public boolean italic;
	public boolean underline;
	public Blink blink = Blink.NONE;
	public boolean inverse;
	public boolean invisible;

	//-------------------------------------------------------------------
	public Style(RGB foreground, RGB background) {
		this.foreground = foreground;
		this.background = background;
	}

	//-------------------------------------------------------------------
	public Style(Style copy) {
		this.foreground = copy.foreground;
		this.background = copy.background;
		this.weight = copy.weight;
		this.italic = copy.italic;
		this.underline = copy.underline;
		this.blink  = copy.blink;
	}

	//-------------------------------------------------------------------
	public void reset() {
		this.foreground=RGB.TRANSPARENT;
		this.background=RGB.TRANSPARENT;
		weight = FontWeight.REGULAR;
		italic=false;
		underline=false;
		blink = Blink.NONE;
	}
}
