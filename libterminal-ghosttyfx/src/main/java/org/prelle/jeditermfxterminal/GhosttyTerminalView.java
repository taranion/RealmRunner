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
import java.util.function.Consumer;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.jeditermfxterminal.impl.SwitchableInputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import io.github.vlaaad.ghosttyfx.Terminal;
import io.github.vlaaad.ghosttyfx.TerminalView;
import javafx.scene.Node;

/**
 *
 */
public class GhosttyTerminalView implements TerminalEmulator, Terminal {

	private final static System.Logger logger = System.getLogger("jedi.terminal");
	
	private SwitchableInputStream inPipe;
	private SwitchableOutputStream outPipe;

	private TerminalView widget;
    private ANSIOutputStream out;
    private ANSIInputStream in;
    
    private int terminalWidth = -1;
    private int terminalHeight = -1;
    private List<Consumer<int[]>> consoleSizeListeners = new ArrayList<>();

	//-------------------------------------------------------------------
	public GhosttyTerminalView() {
		inPipe = new SwitchableInputStream();
		outPipe = new SwitchableOutputStream();

		widget = new TerminalView( (columns,rows) -> {
			logger.log(Level.DEBUG, "TerminalView<init> create terminal with {0}x{1}", columns, rows);
			terminalWidth = columns-3;
			terminalHeight = rows;
			return this;
		});

		out = new ANSIOutputStream(outPipe);
		in  = new ANSIInputStream(inPipe);
		in.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> input: {0}={1}", k, v));
		out.setLoggingListener( (k,v) -> logger.log(Level.ERROR, "GhosttyTerminalView<init> input: {0}={1}", k, v));

//		getBackground();
//		setBackground(Background.fill(backgroundColor.get()));
		initInteractivity();
//		calculateWindowSize();
//		refresh();
		logger.log(Level.DEBUG, "TerminalView<init> done");
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#connectWith(java.io.InputStream, java.io.InputStream)
	 */
	@Override
	public void connectWith(InputStream in, OutputStream out) {
		outPipe.setSink(out);
		inPipe.setSource(in);
		
		try {
			logger.log(Level.WARNING, "Output: "+output());
			logger.log(Level.WARNING, "Input: "+input());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//-------------------------------------------------------------------
	private void initInteractivity() {
		widget.terminalSizeProperty().addListener( (_,_,n) -> {
			if (n!=null) {
				int w = n.columns();
				int h = n.rows();
				if (w!=terminalWidth || h!=terminalHeight) {
					terminalWidth = w;
					terminalHeight = h;
					logger.log(Level.DEBUG, "terminal size changed to {0}x{1}", w, h);
					for (Consumer<int[]> listener : consoleSizeListeners) {
						listener.accept(new int[] {w,h});
					}
				}
			}
		});
	}

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

	@Override
	public ANSIOutputStream getOutputStream() {
		return out;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getInputStream()
	 */
	@Override
	public ANSIInputStream getInputStream() {
		// TODO Auto-generated method stub
		return in;
	}

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

	public Node getPane() {
		return widget;
	}

	//-------------------------------------------------------------------
	/**
	 * @see io.github.vlaaad.ghosttyfx.Terminal#output()
	 */
	@Override
	public InputStream output() throws Exception {
		return inPipe;
	}

	//-------------------------------------------------------------------
	/**
	 * @see io.github.vlaaad.ghosttyfx.Terminal#input()
	 */
	@Override
	public OutputStream input() throws Exception {
		return outPipe;
	}

	@Override
	public void resize(int columns, int rows) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#sendUserInput(java.lang.String)
	 */
	@Override
	public void sendUserInput(String text) {
		byte[] data = ("\u001B[1;33m"+text+"\u001B[0m\r\n").getBytes(Charset.defaultCharset());
		inPipe.inject(data);
		widget.sendText(text+"\r\n");
	}

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

}
