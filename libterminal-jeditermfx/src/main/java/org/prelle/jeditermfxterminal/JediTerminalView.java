/**
 *
 */
package org.prelle.jeditermfxterminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import org.prelle.ansi.FilteringANSIStream;
import org.prelle.terminal.SwitchableInputStream;
import org.prelle.terminal.SwitchableOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;

import javafx.scene.layout.Pane;

/**
 *
 */
public class JediTerminalView implements TerminalEmulator {

	private final static System.Logger logger = System.getLogger("jedi.terminal");
	
	private SwitchableInputStream inPipe;
	private SwitchableOutputStream outPipe;

	private JediTermFxWidget widget;
	private JediTtyConnector connector;
//    private ANSIOutputStream out;
//    private ANSIInputStream in;
    
    private int terminalWidth = -1;
    private int terminalHeight = -1;
    private List<Consumer<int[]>> consoleSizeListeners = new ArrayList<>();

	//-------------------------------------------------------------------
	public JediTerminalView() {
		inPipe = new SwitchableInputStream();
		outPipe = new SwitchableOutputStream();
		connector = new JediTtyConnector();

		widget = new JediTermFxWidget(80, 24, new DefaultSettingsProvider());
		widget.getPane().setMinSize(640, 400);
		widget.setTtyConnector(connector);

//		out = new ANSIOutputStream(new JediTtyConnector.ConnectorOutputStream(connector));
//		in  = new ANSIInputStream(new JediTtyConnector.ConnectorInputStream(connector));

		initListener();
		logger.log(Level.DEBUG, "TerminalView<init> done");
        listenForConsoleSizeChanges();
	}

	//-------------------------------------------------------------------
	private void listenForConsoleSizeChanges() {
		TimerTask updateNAWSTask = new TimerTask() {
			public void run() {
				try {
					int[] size = getConsoleSize();
					boolean changed = (size!=null) && (size[0]!=terminalWidth || size[1]!=terminalHeight);
					if (size!=null) {
						terminalWidth = size[0];
						terminalHeight= size[1];
					}
					if (changed ) {
						logger.log(Level.DEBUG, "Window size changed");
						for (Consumer<int[]> listener : consoleSizeListeners) {
							try {
								listener.accept(size);
							} catch (Exception e) {
								logger.log(Level.ERROR, "Error invoking console size listener",e);
							}
						}
					}
				} catch (Exception e) {
					logger.log(Level.ERROR, "Failed for NAWS update",e);
				}
			}
		};

		Timer timer = new Timer("polling", true);
		timer.schedule(updateNAWSTask, 0, 500);
	}

	//-------------------------------------------------------------------
	public Pane getPane() { return widget.getPane(); }

//	//-------------------------------------------------------------------
//	public ObjectProperty<Terminal> terminalProperty() { return model; }
//	public Terminal getTerminal() { return model.get(); }
//
//	//-------------------------------------------------------------------
//	public ReadOnlyObjectProperty<Integer> columnsProperty() { return columns; }
//	public Integer getColumns() { return columns.get(); }
//
//	//-------------------------------------------------------------------
//	public ReadOnlyObjectProperty<Integer> rowsProperty() { return rows; }
//	public Integer getRows() { return rows.get(); }
//
//	//-------------------------------------------------------------------
//	public BooleanProperty force9x16Property() { return force9x16; }
//	public boolean getForce9x16() { return force9x16.get(); }
//	public TerminalView setForce9x16(boolean value) { this.force9x16.setValue(value); return this; }
//
//	//-------------------------------------------------------------------
//	public ReadOnlyObjectProperty<Font> fontProperty() { return font; }
//	public Font getFont() { return font.get(); }
//	public TerminalView impl_setFont(Font value) { this.font.setValue(value); return this; }
//
//	//-------------------------------------------------------------------
//	public ObjectProperty<Paint> foregroundColorProperty() { return foregroundColor; }
//	public Paint getForegroundColor() { return foregroundColor.get(); }
//	public TerminalView setForegroundColor(Paint value) { this.foregroundColor.setValue(value); return this; }
//
//	//-------------------------------------------------------------------
//	public ObjectProperty<Paint> backgroundColorProperty() { return backgroundColor; }
//	public Paint getBackgroundColor() { return backgroundColor.get(); }
//	public TerminalView setBackgrounddColor(Paint value) { this.backgroundColor.setValue(value); return this; }

//	//-------------------------------------------------------------------
//	private void calculateWindowSize() {
//		int c = (int)( getWidth()  /font.get().getSize() );
//		int r = (int)( getHeight()  /font.get().getSize() );
//		if (columns.get()!=c)
//			columns.setValue(c);
//		if (rows.get()!=r)
//			rows.setValue(r);
//		logger.log(Level.INFO, "Size is {0}x{1} of {2}", columns.get(), rows.get(), this);
//		if (listener!=null) {
//			try {
//				listener.viewportChanged(this, c, r);
//			} catch (Exception e) {
//				logger.log(Level.ERROR, "Error changing viewport size",e);
//			}
//		}
//
//		refresh();
//	}

	//-------------------------------------------------------------------
	private void initListener() {
//		widthProperty().addListener( (ov,o,n) -> calculateWindowSize());
//		heightProperty().addListener( (ov,o,n) -> calculateWindowSize());
//		fontProperty().addListener( (ov,o,n) -> {
//			logger.log(Level.INFO, "Font changed to "+n);
////			try {
////				throw new RuntimeException("Trace");
////			} catch (Exception e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
//			calculateWindowSize();
//		});
	}

	@Override
	public TerminalMode getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLocalEchoActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TerminalEmulator setLocalEchoActive(boolean value) {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public ANSIOutputStream getOutputStream() {
//		// TODO Auto-generated method stub
//		return out;
//	}
//
//	@Override
//	public ANSIInputStream getInputStream() {
//		// TODO Auto-generated method stub
//		return in;
//	}

	@Override
	public int[] getConsoleSize() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Charset[] getEncodings() {
		// TODO Auto-generated method stub
		return new Charset[] {StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII};
	}

//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.terminal.emulated.delete.ITerminalView#addTerminalListener(org.prelle.terminal.emulated.delete.ITerminalViewListener)
//	 */
//	@Override
//	public void addTerminalListener(ITerminalViewListener callback) {
//		logger.log(Level.INFO, "addTerminalListener {0}", callback);
//		this.listener = callback;
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.terminal.emulated.delete.ITerminalView#update(int, int, org.prelle.terminal.emulated.delete.CharInfo)
//	 */
//	@Override
//	public void update(int x, int y, CharInfo glyph) {
//		((FXTerminalSkin)getSkin()).drawText(x, y, glyph.getGlyph(), glyph.getStyle());
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * Redraw
//	 */
//	public void refresh() {
////		logger.log(Level.DEBUG, "create RECREATE event in "+getProperties());
//		if (getProperties().containsKey(Properties.RECREATE))
//			getProperties().remove(Properties.RECREATE);
//        getProperties().put(Properties.RECREATE, Boolean.TRUE);
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.terminal.emulated.delete.ITerminalView#clear()
//	 */
//	@Override
//	public void clear() {
//		logger.log(Level.DEBUG, "create CLEAR event");
//        getProperties().put(Properties.CLEAR, Boolean.TRUE);
//	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#addConsoleSizeListener(java.util.function.Consumer)
	 */
	@Override
	public void addConsoleSizeListener(Consumer<int[]> listener) {
		if (!consoleSizeListeners.contains(listener)) {
			consoleSizeListeners.add(listener);
		}
	}

	@Override
	public FilteringANSIStream connectWith(InputStream in, OutputStream out) {
//		this.in = in;
//		this.out = out;
		outPipe.setSink(out);
		inPipe.setSource(in);
		
		try {
			logger.log(Level.WARNING, "Output: "+this.outPipe);
			logger.log(Level.WARNING, "Input : "+this.inPipe);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void sendUserInput(String text) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void releaseInputBuffer() {
		// TODO Auto-generated method stub
		
	}

}
