package org.prelle.fxterminal;

import java.util.List;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * 
 */
public class TerminalCellData {

	private char glyph;
	private Color foregroundColor;
	private Color backgroundColor;
	private List<TextStyle> styles;
	
	private transient WritableImage cache;
}
