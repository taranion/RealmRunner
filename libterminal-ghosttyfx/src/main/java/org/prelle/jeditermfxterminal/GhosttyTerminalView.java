/**
 *
 */
package org.prelle.jeditermfxterminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.ControlSequenceFragment;
import org.prelle.ansi.FilteringANSIStream;
import org.prelle.ansi.NewANSIInputStream;
import org.prelle.ansi.PrintableFragment;
import org.prelle.ansi.commands.ResetMode;
import org.prelle.ansi.commands.SetMode;
import org.prelle.ansi.commands.SetMode.ANSIMode;
import org.prelle.terminal.EchoChamber;
import org.prelle.terminal.SwitchableInputStream;
import org.prelle.terminal.SwitchableOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import io.github.vlaaad.ghosttyfx.Terminal;
import io.github.vlaaad.ghosttyfx.TerminalView;
import javafx.scene.control.ContextMenu;

/**
 *
 */
public class GhosttyTerminalView implements TerminalEmulator, Terminal {

	private final static System.Logger logger = System.getLogger("ghostty");
	
	private SwitchableInputStream inPipe;
	private SwitchableOutputStream outPipe;

	private TerminalView widget;
    
    private int terminalWidth = -1;
    private int terminalHeight = -1;
    private List<Consumer<int[]>> consoleSizeListeners = new ArrayList<>();
    
    //private PassthroughANSIInputStream pin;
    private NewANSIInputStream pin;
    private boolean localEcho = true;
    private ContextMenu contextMenu;
    private EchoChamber echoChamber;

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
		widget.setMaxHeight(Double.MAX_VALUE);
		widget.setMaxWidth(Double.MAX_VALUE);

		initInteractivity();
		logger.log(Level.DEBUG, "TerminalView<init> done");
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#connectWith(java.io.InputStream, java.io.InputStream)
	 */
	@Override
	public FilteringANSIStream connectWith(InputStream in, OutputStream out) {
		//pin = new PassthroughANSIInputStream(in);
		pin = new NewANSIInputStream(in);
		outPipe.setSink(out);
		//PassOutANSIOutputStream pout = new PassOutANSIOutputStream(out, (fragmentSent) -> handleFragmentSent(fragmentSent));
//		echoChamber = new EchoChamber(out, inPipe);
//		echoChamber.setEchoEnabled(this.localEcho);
//		outPipe.setSink(echoChamber);
		inPipe.setSource(pin);
		
		
		try {
			logger.log(Level.WARNING, "Output: "+this.outPipe);
			logger.log(Level.WARNING, "Input : "+this.inPipe);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pin;
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#start()
	 */
	@Override
	public void start() {
		logger.log(Level.INFO, "start() called");
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
//		widget.setOnMouseClicked(ev -> {
//			logger.log(Level.INFO, "Mouse clicked on console {0}", ev);
//			if (contextMenu!=null && contextMenu.isShowing()) {
//				contextMenu.hide();
//			}
//			
//			if (ev.getButton()==javafx.scene.input.MouseButton.SECONDARY) {
//				// Copy selection to system clipboard and read from it
//				widget.copySelection();
//				// Read from clipboard
//				String selectedText = Clipboard.getSystemClipboard().getString();
//				logger.log(Level.INFO, "Read "+selectedText);
//				if (selectedText!=null && !selectedText.isEmpty()) {
//					// Show pop up menu 
//					contextMenu = new ContextMenu();
//					MenuItem copy = new MenuItem("Copy");
//					MenuItem trigger = new MenuItem("Trigger definieren");
//					contextMenu.getItems().addAll(copy, trigger);
//					copy.setOnAction(new EventHandler<ActionEvent>() {
//					    @Override
//					    public void handle(ActionEvent event) {
//					        System.out.println("Cut...");
//					        contextMenu.hide();
//					    }
//					});
//					trigger.setOnAction(new EventHandler<ActionEvent>() {
//					    @Override
//					    public void handle(ActionEvent event) {
//					        System.out.println("trigger...");
//					        contextMenu.hide();
//					    }
//					});
//					contextMenu.show(widget, ev.getScreenX(), ev.getScreenY());
//					
////					
////					Popup popup = new Popup();
////					popup.getContent().add(contextMenu);
//				}
//			}
//			
//		});
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
		return localEcho;
	}

	@Override
	public TerminalEmulator setLocalEchoActive(boolean value) {
		logger.log(Level.INFO, "setLocalEchoActive({0}) called", value);
		this.localEcho = value;
		if (echoChamber != null) {
			echoChamber.setEchoEnabled(value);
		} else {
			logger.log(Level.INFO, "setLocalEchoActive({0}) called before echoChamber was initialized", value);
		}
		
        // Inject ANSI SRM (SendReceiveMode) control sequence into terminal input stream
        ControlSequenceFragment srmSequence = value
            ? new ResetMode(ANSIMode.SRM_SEND_RECEIVE_MODE)  // ESC [ 1 2 l -> Local Echo ON
            : new SetMode(ANSIMode.SRM_SEND_RECEIVE_MODE);   // ESC [ 1 2 h -> Local Echo OFF

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        srmSequence.encode(baos, true);
        if (inPipe != null) {
            inPipe.inject(baos.toByteArray());
        }
		return this;
	}

//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.terminal.TerminalEmulator#getOutputStream()
//	 */
//	@Override
//	public ANSIOutputStream getOutputStream() {
//		return out;
//	}
//
//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.terminal.TerminalEmulator#getInputStream()
//	 */
//	@Override
//	public ANSIInputStream getInputStream() {
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

	public TerminalView getPane() {
		return widget;
	}

	//-------------------------------------------------------------------
	/**
	 * @see io.github.vlaaad.ghosttyfx.Terminal#output()
	 */
	@Override
	public InputStream output() throws Exception {
		System.err.println("GhosttyTerminalView.output() called and returns "+inPipe);
		return inPipe;
	}

	//-------------------------------------------------------------------
	/**
     * The terminal view reads this stream and writes the received bytes to the
     * terminal emulator.
     *
     * @return the stream that produces terminal output
     * @throws Exception if the output stream cannot be opened
	 * @see io.github.vlaaad.ghosttyfx.Terminal#input()
	 */
	@Override
	public OutputStream input() throws Exception {
		System.err.println("GhosttyTerminalView.input() called and returns "+outPipe);
		return outPipe;
	}

	//-------------------------------------------------------------------
	/**
	 * @see io.github.vlaaad.ghosttyfx.Terminal#resize(int, int, int, int)
	 */
	@Override
	public void resize(int columns, int rows, int widthPx, int heightPx) throws Exception {
		// TODO Auto-generated method stub
		logger.log(Level.INFO, "resize({0}x{1} cells, {2}x{3} pixel) called", columns, rows, widthPx, heightPx);
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
		logger.log(Level.INFO, "sendUserInput: {0}", text);
		byte[] data = (text + "\r\n").getBytes(StandardCharsets.UTF_8);
		try {
			outPipe.write(data);
			outPipe.flush();
		} catch (IOException e) {
			logger.log(Level.ERROR, "Error sending user input to outPipe", e);
		}
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

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#releaseInputBuffer()
	 */
	@Override
	public void releaseInputBuffer() {
		//pin.releaseBuffer();
	}

	//-------------------------------------------------------------------
	private void handleFragmentSent(AParsedElement fragmentSent) {
		if (localEcho) {
			// Local echo for Printable, CO and C1 codes
			if (fragmentSent instanceof PrintableFragment) {
				inPipe.inject(fragmentSent.getRaw());
			}
		}
	}
}
