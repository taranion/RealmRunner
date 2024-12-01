package org.prelle.terminal.emulated;

/**
 * This represents a single addressable character cell
 */
public class Cell {
	
	private Character glyph = null;
//	private String backRGB;
//	private String foreRGB;

	//-------------------------------------------------------------------
	public Cell() {
	}

	//-------------------------------------------------------------------
	public Character getGlyph() { return glyph; }
	public void setGlyph(Character glyph) { this.glyph = glyph; }

}
