package org.prelle.fxterminal.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javafx.geometry.Bounds;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;

public class FontMetrics {

	private final static Logger logger = System.getLogger("fxterminal");

	final private Text internal;

	public double ascent, descent, lineHeight, width;
	public double pre,post;

	public FontMetrics(Font fnt) {
		double dpi = Screen.getPrimary().getDpi();
		logger.log(Level.DEBUG, "DPI = "+dpi);

		internal =new Text("\u2588");
		//internal =new Text("j");
		internal.setFont(fnt);
		Bounds b= internal.getLayoutBounds();
		logger.log(Level.DEBUG,"FontMetrics.<init>: size="+fnt.getSize()+" bounds="+b);
		lineHeight= b.getHeight();
		ascent= -b.getMinY();
		descent= b.getMaxY();
		pre   = -b.getMinX();
		post  =  b.getMaxX();
		width =  b.getWidth();
	}

	public String toString() {
		return "Metric(lineHeight="+lineHeight+", ascent="+ascent+", descent="+descent+", width="+width+", pre"+pre+", post="+post+")";
	}

	public float computeStringWidth(String txt) {
		internal.setText(txt);
		return (float) internal.getLayoutBounds().getWidth();
	}
}
