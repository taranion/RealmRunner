package org.prelle.fxterminal.impl;

import org.prelle.terminal.emulated.Style;

import javafx.scene.paint.Paint;

/**
 *
 */
public interface TerminalGraphic {

	//-------------------------------------------------------------------
	public void clear();

	//-------------------------------------------------------------------
	public void fillBackground(double x, double y, double toX, double toY, Paint backgroundColor);

	//-------------------------------------------------------------------
	public void drawText(int x, int y, String text, Style style);

}
