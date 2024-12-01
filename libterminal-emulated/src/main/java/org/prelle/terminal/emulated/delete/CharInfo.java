package org.prelle.terminal.emulated.delete;

public class CharInfo {

	public final static CharInfo EMPTY = new CharInfo(null,new Style(null,null));

	private String glyph;
	private Style style;

//	public CharInfo(char c) { this.glyph=String.valueOf(c); this.style=new Style(null,null); }
	public CharInfo(String glyph) { this.glyph=glyph; style=new Style(Style.DEFAULT);}
	public CharInfo(String glyph, Style style) { this.glyph=glyph; this.style=new Style(style);}

	//-------------------------------------------------------------------
	public String getGlyph() {
		return glyph;
	}
	//-------------------------------------------------------------------
	public void setGlyph(String value, Style style) {
		this.glyph=value;
		this.style=new Style(style);
		if (style==null)
			throw new NullPointerException();
	}

	//-------------------------------------------------------------------
	public Style getStyle() {
		return style;
	}

	//-------------------------------------------------------------------
	public void clear() {
		glyph=null;
		style.reset();

	}

}