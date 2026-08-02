/**
 *
 */
package org.prelle.ghostty;

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
import org.prelle.ansi.C0Code;
import org.prelle.ansi.FilteringANSIStream;
import org.prelle.terminal.FromServerToTerminal;
import org.prelle.terminal.FromTerminalToServer;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import io.github.vlaaad.ghosttyfx.TerminalView;
import javafx.scene.control.ContextMenu;

/**
 *
 */
public class GhosttyTerminalView2 implements TerminalEmulator {

	private final static System.Logger logger = System.getLogger("ghostty");

	private Charset encoding = StandardCharsets.UTF_8;

	private GhosttyTerminalConnector connector;
	private TerminalView widget;
	   
    private FromServerToTerminal fromServer;
	private FromTerminalToServer fromTerminal;
   
    private int terminalWidth = -1;
    private int terminalHeight = -1;
    private List<Consumer<int[]>> consoleSizeListeners = new ArrayList<>();
    
    private ANSIOutputStream out;
    private ANSIInputStream pin;
    private ContextMenu contextMenu;

	//-------------------------------------------------------------------
	public GhosttyTerminalView2() {
		try {
			connector = new GhosttyTerminalConnector(StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		widget = new TerminalView( (columns,rows) -> {
			logger.log(Level.DEBUG, "TerminalView<init> create terminal with {0}x{1}", columns, rows);
			terminalWidth = columns-3;
			terminalHeight = rows;
			return connector;
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
		logger.log(Level.INFO, "ENTER: connectWith");
		pin = new ANSIInputStream(in);
		this.out = new ANSIOutputStream(out);
		
		try {
			logger.log(Level.WARNING, "Output: "+this.out);
			logger.log(Level.WARNING, "Input : "+this.pin);
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
		// Start a thread that reads from the MUD and writes to the terminal
		fromServer = new FromServerToTerminal(pin, connector.getWriteToTerminal(), encoding);
		fromTerminal = new FromTerminalToServer(connector.getReadByServer(), out);
		fromTerminal.setEchoStream(connector.getWriteToTerminal());
		fromTerminal.setLocalEcho(true);
		
		try {
//			widget.setTtyConnector(connector);
			logger.log(Level.WARNING, "Output: "+pin);
			logger.log(Level.WARNING, "Input : "+out);
			Thread t = new Thread(fromServer, "FromServerToTerminal");
			t.start();
			Thread t2 = new Thread(fromTerminal, "FromTerminalToServer");
			t2.start();
			
			Thread.sleep(200);
			connector.getWriteToTerminal().write("Welcome to the JediTermFX Terminal Emulator!\r\n".getBytes(encoding));
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
		return fromTerminal.isLocalEcho();
	}

	@Override
	public TerminalEmulator setLocalEchoActive(boolean value) {
		logger.log(Level.INFO, "{0} local echo", value ? "Enable" : "Disable");
		fromTerminal.setLocalEcho(value);
		return this;
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

	//-------------------------------------------------------------------
	public TerminalView getPane() {
		return widget;
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
			out.write(text);
			out.write(C0Code.CR);
			out.write(C0Code.LF);
			out.flush();
			
			if (isLocalEchoActive()) {
				connector.getWriteToTerminal().write(data);
				connector.getWriteToTerminal().flush();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

}
