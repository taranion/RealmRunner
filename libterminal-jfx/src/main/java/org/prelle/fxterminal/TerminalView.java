/**
 *
 */
package org.prelle.fxterminal;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.prelle.fxterminal.impl.FXTerminalSkin;
import org.prelle.fxterminal.impl.Properties;
import org.prelle.terminal.emulated.Terminal;
import org.prelle.terminal.emulated.delete.CharInfo;
import org.prelle.terminal.emulated.delete.ITerminalView;
import org.prelle.terminal.emulated.delete.ITerminalViewListener;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 *
 */
public class TerminalView extends Control implements ITerminalView {

	public static class FXTerminalBuilder {
		private Terminal terminal;
		private Font font;
		private boolean force;

		public FXTerminalBuilder showTerminal(Terminal value) { this.terminal=value; return this; }
		public FXTerminalBuilder setFont(Font value) { this.font=value; return this; }
		public FXTerminalBuilder setForce9x16(boolean value) { this.force=value; return this; }
		public TerminalView build() {
			TerminalView ret = new TerminalView(terminal);
			//if (font!=null) terminal.setFont(font);
			if (terminal!=null) terminal.setView(ret);
			ret.setForce9x16(force);
			return ret;
		}

	}

	private final static Logger logger = System.getLogger("fxterminal");

	private ObjectProperty<Integer> columns = new SimpleObjectProperty<>(0);
	private ObjectProperty<Integer> rows    = new SimpleObjectProperty<>(0);

	private ObjectProperty<Paint> backgroundColor = new SimpleObjectProperty<>(Color.valueOf("#101010"));
	ObjectProperty<Paint> foregroundColor = new SimpleObjectProperty<>(Color.valueOf("#808080"));
	private ObjectProperty<Font>  font = new SimpleObjectProperty<Font>(Font.font("Monospaced Regular", 12));
	private BooleanProperty force9x16= new SimpleBooleanProperty();

	private ObjectProperty<Terminal> model  = new SimpleObjectProperty<Terminal>();
	private ITerminalViewListener listener;

	//-------------------------------------------------------------------
	public static FXTerminalBuilder builder() {
		return new FXTerminalBuilder();
	}

	//-------------------------------------------------------------------
	public TerminalView(Terminal terminal) {
		model.set(terminal);
		if (terminal!=null)
			terminal.setView(this);
		setMinSize(640, 400);
		getBackground();
		setBackground(Background.fill(backgroundColor.get()));
		initListener();
		calculateWindowSize();
		refresh();
		logger.log(Level.DEBUG, "TerminalView<init> done");
	}

	//-------------------------------------------------------------------
	/**
	 * @see javafx.scene.control.Control#createDefaultSkin()
	 */
	@Override protected Skin<?> createDefaultSkin() {
		FXTerminalSkin skin = new FXTerminalSkin(this);
//		skin.refresh();
//		logic = new TerminalLogic(skin, this);
		return skin;
    }

	//-------------------------------------------------------------------
	public ObjectProperty<Terminal> terminalProperty() { return model; }
	public Terminal getTerminal() { return model.get(); }

	//-------------------------------------------------------------------
	public ReadOnlyObjectProperty<Integer> columnsProperty() { return columns; }
	public Integer getColumns() { return columns.get(); }

	//-------------------------------------------------------------------
	public ReadOnlyObjectProperty<Integer> rowsProperty() { return rows; }
	public Integer getRows() { return rows.get(); }

	//-------------------------------------------------------------------
	public BooleanProperty force9x16Property() { return force9x16; }
	public boolean getForce9x16() { return force9x16.get(); }
	public TerminalView setForce9x16(boolean value) { this.force9x16.setValue(value); return this; }

	//-------------------------------------------------------------------
	public ReadOnlyObjectProperty<Font> fontProperty() { return font; }
	public Font getFont() { return font.get(); }
	public TerminalView impl_setFont(Font value) { this.font.setValue(value); return this; }

	//-------------------------------------------------------------------
	public ObjectProperty<Paint> foregroundColorProperty() { return foregroundColor; }
	public Paint getForegroundColor() { return foregroundColor.get(); }
	public TerminalView setForegroundColor(Paint value) { this.foregroundColor.setValue(value); return this; }

	//-------------------------------------------------------------------
	public ObjectProperty<Paint> backgroundColorProperty() { return backgroundColor; }
	public Paint getBackgroundColor() { return backgroundColor.get(); }
	public TerminalView setBackgrounddColor(Paint value) { this.backgroundColor.setValue(value); return this; }

	//-------------------------------------------------------------------
	private void calculateWindowSize() {
		int c = (int)( getWidth()  /font.get().getSize() );
		int r = (int)( getHeight()  /font.get().getSize() );
		if (columns.get()!=c)
			columns.setValue(c);
		if (rows.get()!=r)
			rows.setValue(r);
		logger.log(Level.INFO, "Size is {0}x{1} of {2}", columns.get(), rows.get(), this);
		if (listener!=null) {
			try {
				listener.viewportChanged(this, c, r);
			} catch (Exception e) {
				logger.log(Level.ERROR, "Error changing viewport size",e);
			}
		}

		refresh();
	}

	//-------------------------------------------------------------------
	private void initListener() {
		widthProperty().addListener( (ov,o,n) -> calculateWindowSize());
		heightProperty().addListener( (ov,o,n) -> calculateWindowSize());
		fontProperty().addListener( (ov,o,n) -> {
			logger.log(Level.INFO, "Font changed to "+n);
//			try {
//				throw new RuntimeException("Trace");
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			calculateWindowSize();
		});
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.emulated.delete.ITerminalView#addTerminalListener(org.prelle.terminal.emulated.delete.ITerminalViewListener)
	 */
	@Override
	public void addTerminalListener(ITerminalViewListener callback) {
		logger.log(Level.INFO, "addTerminalListener {0}", callback);
		this.listener = callback;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.emulated.delete.ITerminalView#update(int, int, org.prelle.terminal.emulated.delete.CharInfo)
	 */
	@Override
	public void update(int x, int y, CharInfo glyph) {
		((FXTerminalSkin)getSkin()).drawText(x, y, glyph.getGlyph(), glyph.getStyle());
	}

	//-------------------------------------------------------------------
	/**
	 * Redraw
	 */
	public void refresh() {
//		logger.log(Level.DEBUG, "create RECREATE event in "+getProperties());
		if (getProperties().containsKey(Properties.RECREATE))
			getProperties().remove(Properties.RECREATE);
        getProperties().put(Properties.RECREATE, Boolean.TRUE);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.emulated.delete.ITerminalView#clear()
	 */
	@Override
	public void clear() {
		logger.log(Level.DEBUG, "create CLEAR event");
        getProperties().put(Properties.CLEAR, Boolean.TRUE);
	}

}
