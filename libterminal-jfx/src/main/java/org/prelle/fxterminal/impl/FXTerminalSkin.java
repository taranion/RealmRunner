package org.prelle.fxterminal.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.prelle.fxterminal.TerminalView;
import org.prelle.terminal.emulated.delete.CharInfo;
import org.prelle.terminal.emulated.delete.Style;
import org.prelle.terminal.emulated.delete.Style.RGB;
import org.prelle.terminal.emulated.TerminalModel;

import static org.prelle.fxterminal.impl.Properties.RECREATE;
import static org.prelle.fxterminal.impl.Properties.CLEAR;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.geometry.VPos;
import javafx.scene.control.SkinBase;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class FXTerminalSkin extends SkinBase<TerminalView> implements TerminalGraphic {

	private final static Logger logger = System.getLogger("fxterminal");

	private Font defaultMonospaced= Font.font("Monospace", 12);;
	private Font font9x16= Font.loadFont(ClassLoader.getSystemResourceAsStream("MxPlus_IBM_VGA_9x16-2x.ttf"), 12);;

	private ResizeableCanvas canvas;
	private double fontWidth, fontHeight;
	private FontMetrics metrics;

	//-------------------------------------------------------------------
	public FXTerminalSkin(TerminalView control) {
		super(control);

		updateFontMetrics();
		initInteractivity();

		Text chars80 = new Text("12345678901234567890123456789012345678901234567890123456789012345678901234567890");
		chars80.setFont(getSkinnable().getFont());;
		logger.log(Level.INFO,"80 chars should be {0} pixel",chars80.getLayoutBounds().getWidth());
		canvas = new ResizeableCanvas();
		canvas.resize(chars80.getLayoutBounds().getWidth(), 30*fontHeight);
//		canvas.widthProperty().bind(control.widthProperty());
//		canvas.heightProperty().bind(control.heightProperty());
		canvas.getGraphicsContext2D().setFill(control.getForegroundColor());
		canvas.getGraphicsContext2D().setFont(control.getFont());
		logger.log(Level.INFO,"getFont() = "+canvas.getGraphicsContext2D().getFont());

//		canvas.getGraphicsContext2D().fillText("Hallo", 30, 30);

		getChildren().add(canvas);

//		drawText(2,2,"c", new Style(null, null));
	}

	//-------------------------------------------------------------------
	private void initInteractivity() {
		logger.log(Level.INFO, "start listening to properties of "+getSkinnable());
		getSkinnable().getProperties().clear();
        getSkinnable().getProperties().addListener(new MapChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Object, ? extends Object> c) {
            	logger.log(Level.ERROR, "onChanged "+c);
                if (c.wasAdded() && RECREATE.equals(c.getKey())) {
	                refresh();
                    getSkinnable().getProperties().remove(RECREATE);
                }
                if (c.wasAdded() && CLEAR.equals(c.getKey())) {
	                clear();
                    getSkinnable().getProperties().remove(CLEAR);
                }
            }
        });
		getSkinnable().fontProperty().addListener( (ov,o,n) -> updateFontMetrics());
	}

	//-------------------------------------------------------------------
	private void updateFontMetrics() {
		logger.log(Level.WARNING, "ENTER updateFontMetrics for {0}", getSkinnable().getFont());
		metrics = new FontMetrics(getSkinnable().getFont());
		logger.log(Level.INFO, "The font {0} has metric {1}",getSkinnable().getFont(),metrics);
		if (getSkinnable().getForce9x16()) {
			fontWidth=9;
			fontHeight=16;
			logger.log(Level.INFO, "Force as {0}x{1}",fontWidth, fontHeight);
		} else {
			fontWidth = metrics.width+1;
			fontHeight= metrics.lineHeight;
			logger.log(Level.INFO, "Calculate font as {0}x{1}",fontWidth, fontHeight);

		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.fxterminal.impl.TerminalGraphic#clear()
	 */
	@Override
	public void clear() {
		canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}

	//-------------------------------------------------------------------
	public void refresh() {
		logger.log(Level.INFO, "refresh");
		TerminalModel model = getSkinnable().getTerminal().getModel();
		for (int y=0; y<model.getHeight(); y++) {
//			logger.log(Level.INFO, "draw line {0}",y);
			for (int x=0; x<model.getWidth(); x++) {
				CharInfo info = model.getGlyphAt(x, y);
				drawText(x,y, info.getGlyph(), info.getStyle());
				//logger.log(Level.DEBUG, "Style at {0}/{2} is {1}",x, info.getStyle().background, info.getGlyph());
			}
		}
	}

	//-------------------------------------------------------------------
	private static Color convert(Style.RGB rgb) {
		if (rgb==RGB.TRANSPARENT) return Color.TRANSPARENT;
		return Color.rgb(rgb.r(), rgb.g(), rgb.b());
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.fxterminal.impl.TerminalGraphic#fillBackground(int, int, int, int, javafx.scene.paint.Paint)
	 */
	@Override
	public void fillBackground(double x, double y, double w, double h, Paint backgroundColor) {
		canvas.getGraphicsContext2D().setFill(backgroundColor);
		canvas.getGraphicsContext2D().fillRect(x, y, w, h);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.fxterminal.impl.TerminalGraphic#drawText(int, int, java.lang.String, org.prelle.terminal.emulated.delete.Style)
	 */
	@Override
	public void drawText(int x, int y, String text, Style style) {
		if (style==null)
			throw new NullPointerException("Style may nut be null");
		canvas.getGraphicsContext2D().setFont(defaultMonospaced);
		double realX = Math.ceil(x*fontWidth);
		double realY = y*fontHeight;
		double w = Math.ceil(text.length()*fontWidth);
		double h = fontHeight;
//		logger.log(Level.INFO, "drawText {0} at {1}x{2}   (Font = {3}x{4})",text, realX, realY, fontWidth, fontHeight);
		if (style!=null && style.background!=null) {
//			logger.log(Level.INFO, "fill background with "+style.background);
			fillBackground(realX, realY, w, h, convert(style.background));
		} else {
//			logger.log(Level.INFO, "clear {0} at {1}x{2}   with bg={3}",text, realX, realY, (style!=null)?style.background:"null");
			canvas.getGraphicsContext2D().clearRect(realX, realY, w, h);
		}

//		logger.log(Level.INFO, "drawing at {0} really is {1}", x, realX);
		if (style.foreground!=null) {
			canvas.getGraphicsContext2D().setFill(convert(style.foreground));
			canvas.getGraphicsContext2D().setFont(getSkinnable().getFont());
			canvas.getGraphicsContext2D().setTextBaseline(VPos.BOTTOM);
			double baseLine = realY+fontHeight;//+fontHeight-metrics.descent+1.3;
//			logger.log(Level.INFO, "drawText {0} at {1}x{2}  baseline={3}",text, realX, baseLine, canvas.getGraphicsContext2D().getTextBaseline());
			canvas.getGraphicsContext2D().fillText(text, realX, baseLine);
		}

//		canvas.getGraphicsContext2D().setFill(Color.LIGHTGRAY);
//		canvas.getGraphicsContext2D().fillText("g", realX, baseLine);
	}

	//-------------------------------------------------------------------
	public void drawGrid() {
		logger.log(Level.INFO, "drawGrid for fontsize {0}x{1} in canvas {2}x{3}  metrics={4}", fontWidth,fontHeight, canvas.getWidth(), canvas.getHeight(), metrics);
		canvas.getGraphicsContext2D().setFill(Color.LIGHTGRAY);
		canvas.getGraphicsContext2D().setStroke(Color.YELLOW);
		canvas.getGraphicsContext2D().setLineWidth(1);
		for (int y=1; y<=80; y++) {
			canvas.getGraphicsContext2D().moveTo(0, y*fontHeight);
			canvas.getGraphicsContext2D().lineTo(canvas.getWidth(), y*fontHeight);
			canvas.getGraphicsContext2D().stroke();
		}
		for (int x=1; x<=80; x++) {
			canvas.getGraphicsContext2D().moveTo(x*fontWidth, 0);
			canvas.getGraphicsContext2D().lineTo(x*fontWidth, canvas.getHeight());
			canvas.getGraphicsContext2D().stroke();
		}

	}
}