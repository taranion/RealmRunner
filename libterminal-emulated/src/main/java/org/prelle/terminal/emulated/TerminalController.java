package org.prelle.terminal.emulated;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.List;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.C0Fragment;
import org.prelle.ansi.ControlSequenceFragment;
import org.prelle.ansi.PrintableFragment;
import org.prelle.terminal.emulated.delete.ColorPalette;
import org.prelle.terminal.emulated.delete.Emulation;
import org.prelle.terminal.emulated.delete.ITerminalView;
import org.prelle.terminal.emulated.delete.ITerminalViewListener;
import org.prelle.terminal.emulated.delete.Style;
import org.prelle.terminal.emulated.delete.Style.Blink;
import org.prelle.terminal.emulated.delete.Style.FontWeight;
import org.prelle.terminal.emulated.delete.Style.RGB;

/**
 *
 */
public abstract class TerminalController implements ITerminalViewListener {

	public static enum LineFeedMode {
		/** LF, FF or VT moves cursor to first column */
		NEW_LINE,
		/** LF, FF or VT moves cursor just to new line, but keeps column */
		LINE_FEED
	}

	protected final static Logger logger = Constants.logger;

	private RGB[] colorPalette = ColorPalette.VGA;
	private TerminalModel model;
	private int caretX, caretY;
	private int viewportHeight = 24;
	private LineFeedMode lfm = LineFeedMode.NEW_LINE;

	private Style style;

	//-------------------------------------------------------------------
	protected TerminalController(TerminalModel model, Emulation emul) {
		this.model = model;
		style = new Style(RGB.WHITE, RGB.TRANSPARENT);
	}

	//-------------------------------------------------------------------
	public void setColorPalette(RGB[] value) {
		this.colorPalette = value;
	}

	//-------------------------------------------------------------------
	public void clear() {
		model.clear();
		caretX = 0;
		caretY = 0;
	}

	//-------------------------------------------------------------------
	protected void handleFragment(AParsedElement fragment) {
//		logger.log(Level.WARNING, "handleFragment {0}", fragment);
		switch (fragment) {
		case PrintableFragment tf:
			drawCharacters(tf.getText());
			break;
		case C0Fragment c0:
			switch (c0.getCode()) {
			case LF:
//				logger.log(Level.DEBUG, "Linefeed in line {0}", caretY);
				nextLine();
				if (lfm==LineFeedMode.NEW_LINE)
					caretX=0;
				break;
			case VT:
				// Vertical tab
				logger.log(Level.TRACE, "Vertical tab in line {0}", caretY);
				nextLine();
				if (lfm==LineFeedMode.NEW_LINE)
					caretX=0;
				break;
			case CR:
//				logger.log(Level.DEBUG, "Carriage Return in column {0}", caretX);
				caretX=0;
				break;
			default:
				logger.log(Level.WARNING, "Unhandled C0 {0}", c0.getCode());
			}
			break;
		case ControlSequenceFragment sgr:
			processSGR(sgr);
//			logger.log(Level.INFO, "change Color {0}", fragment);
			break;
		default:
			break;
		}
	}

	//-------------------------------------------------------------------
	private void processSGR(ControlSequenceFragment sgr) {
		List<Integer> params = sgr.getArguments();
//		logger.log(Level.INFO, "Params = "+params);
		for (int i=0; i<params.size(); i++) {
			int command = params.get(i);
//			logger.log(Level.INFO, "  now "+command+" at index "+i);
			switch ( (Integer)command) {
			case 0: // Reset to normal
				style.blink=Blink.NONE;
				style.italic=false;
				style.weight=FontWeight.REGULAR;
				style.foreground=ColorPalette.VGA[7];
				style.background=RGB.TRANSPARENT;
				break;
			case 1: // Bold
				style.weight = FontWeight.BOLD;
				break;
			case 2: // Faint
				style.weight = FontWeight.LIGHT;
				break;
			case 3: // Italic
				style.italic = true;
				break;
			case 5: style.blink = Blink.SLOW; break;
			case 6: style.blink = Blink.RAPID; break;
			case 7: style.inverse = true; break;
			case 8: style.invisible = true; break;
			case 22: // Normal intensity
				style.weight = FontWeight.REGULAR;
				break;
			case 25: // Not blinking
				style.blink = Blink.NONE;
				break;
			case 27: style.inverse = false; break;
			case 28: style.invisible = false; break;
			case Integer col when col>=30 && col<38:
				style.foreground = colorPalette[col-30];
				break;
			case 38: i=handleSGR38(params,i); break;
			case 39: style.foreground =  RGB.WHITE; break;
			case Integer col when col>=40 && col<48:
				style.background = colorPalette[col-40];
				break;
			case 48: i=handleSGR48(params,i); break;
			case 49: style.background =  RGB.TRANSPARENT; break;
			case Integer col when col>=90 && col<98:
				style.foreground = colorPalette[col-82];
				break;
			case Integer col when col>=100 && col<108:
				style.background = colorPalette[col-92];
				break;

			default:
				logger.log(Level.WARNING, "Unsupported SGR parameter {0}", command);
			}
		}

	}

	//-------------------------------------------------------------------
	private int handleSGR38(List<Integer> params, int oldPos) {
		int what = params.get(oldPos+1);
		if (what==5) {
			// Next code is ANSI 256 color
			int color = params.get(oldPos+2);
			style.foreground = get8BitColor(color);
			return oldPos+2;
		} else if (what==2) {
			// Next 3 codes are RGB
			int r = params.get(oldPos+2);
			int g = params.get(oldPos+3);
			int b = params.get(oldPos+4);
			style.foreground = new RGB(r,g,b);
			return oldPos+4;
		} else {
			logger.log(Level.WARNING, "Don't know how to support {0} after 38 in {1}", what, params);
		}
		return oldPos+1;
	}

	//-------------------------------------------------------------------
	private RGB get8BitColor(int color) {
		try {
			RGB ret = ColorPalette.ANSI[color];
			if (color<16)
				ret = colorPalette[color];
			if (ret==null) {
				logger.log(Level.ERROR, "No background color {0} defined",color);
			}
			return ret;
		} catch (Exception e) {
			logger.log(Level.WARNING, "TODO: set background color "+color);
		}
		return null;
	}

	//-------------------------------------------------------------------
	private int handleSGR48(List<Integer> params, int oldPos) {
		int what = params.get(oldPos+1);
		if (what==5) {
			// Next code is ANSI 256 color
			int color = params.get(oldPos+2);
			style.background = get8BitColor(color);
			return oldPos+2;
		} else if (what==2) {
//			logger.log(Level.WARNING, "RGB: {0}", params);
			// Next 3 codes are RGB
			int r = params.get(oldPos+2);
			int g = params.get(oldPos+3);
			int b = params.get(oldPos+4);
			style.background = new RGB(r,g,b);
			return oldPos+4;
		} else {
			logger.log(Level.WARNING, "Don't know how to support {0} after 38 in {1}", what, params);
		}
		return oldPos+1;
	}

	//-------------------------------------------------------------------
	private void drawCharacters(String text) {
		for (int i=0; i<text.length(); i++) {
			char c = text.charAt(i);
			try {
				model.setGlyphAt(c+"", caretX, caretY, style);
			} catch (Exception e) {
			}
			//graphic.drawText(caretX, caretY, String.valueOf(c), style);
			caretX++;

			if (caretX>=model.getWidth()) {
				logger.log(Level.TRACE, "Wrap");
				newline();
			}
		}
	}

	//-------------------------------------------------------------------
	private void nextLine() {
//		logger.log(Level.DEBUG, "nextline while in {0}",caretY);
		caretY++;
		if (caretY>model.getHeight()) {
			logger.log(Level.WARNING, "ToDo: Scrolling {0}",caretY);
			model.addLine();
		}
	}

	//-------------------------------------------------------------------
	private void newline() {
		if (caretX>0) {
//			nextLine();
			caretX=0;
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.emulated.delete.ITerminalViewListener#viewportChanged(org.prelle.terminal.emulated.delete.ITerminalView, int, int)
	 */
	@Override
	public void viewportChanged(ITerminalView src, int w, int h) {
		logger.log(Level.INFO, "viewport changed to {0}x{1}", w,h);
		viewportHeight = h;

	}
}
