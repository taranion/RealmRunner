package org.prelle.terminal.emulated;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import org.prelle.terminal.emulated.delete.CharInfo;
import org.prelle.terminal.emulated.delete.ITerminalView;
import org.prelle.terminal.emulated.delete.Style;

/**
 *
 */
public class TerminalModel {

	private final static Logger logger = Constants.logger;

	private class Line {
		CharInfo[] data;
		public Line(int columns) {
			 data = new CharInfo[columns];
			 for (int i=0; i<data.length; i++) data[i]=new CharInfo(" ", Style.DEFAULT);
		}
		public void clear() {
			for (CharInfo tmp : data) tmp.clear();
		}
	}

	private ITerminalView view;
	private List<Line> lines = new ArrayList<>();
	private int width=80, height=50;

	//-------------------------------------------------------------------
	public TerminalModel() {
		lines = new ArrayList<>();
		for (int i=0; i<height; i++) lines.add(new Line(width));
	}

	//-------------------------------------------------------------------
	/**
	 * @param view
	 */
	public void setTerminalView(ITerminalView view) {
		this.view = view;
	}

	//-------------------------------------------------------------------
	public void clear() {
		lines.forEach(line -> line.clear());
	}

	//-------------------------------------------------------------------
	public void setGlyphAt(String glyph, int x, int y, Style style) {
		if (style==null)
			throw new IllegalArgumentException("Style may not be null");

		//logger.log(Level.DEBUG, "Write glyph ''{0}'' at {1}x{2}   view={3}", glyph, x,y, style.background);
		if (y>=lines.size()) throw new IllegalArgumentException("Cannot write into line "+y+" of "+lines.size());
		Line line = lines.get(y);
		if (x>=line.data.length) throw new IllegalArgumentException("Cannot write into column "+x+" of "+line.data.length);
		if (line.data[x]==null) throw new IllegalArgumentException("No data in "+x+"x"+y);
		line.data[x].setGlyph(glyph, style);

		if (view!=null) {
			view.update(x, y, line.data[x]);
		}
	}

	//-------------------------------------------------------------------
	public CharInfo getGlyphAt(int x, int y) {
		if (y>=lines.size()) throw new IllegalArgumentException("Cannot write into line "+y+" of "+lines.size());
		Line line = lines.get(y);
		if (x>=line.data.length) throw new IllegalArgumentException("Cannot write into column "+x+" of "+line.data.length);
		if (line.data[x]==null) throw new IllegalArgumentException("No data in "+x+"x"+y);
		return line.data[x];
	}

	//-------------------------------------------------------------------
	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	//-------------------------------------------------------------------
	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the height
	 */
	public int getHeight() {
		//return height;
		return lines.size();
	}

	//-------------------------------------------------------------------
	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	//-------------------------------------------------------------------
	public void addLine() {
		lines.add(new Line(width));
		if (lines.size()>30) {
			logger.log(Level.INFO, "Forget a line");
			lines.remove(0);
		}
	}

}
