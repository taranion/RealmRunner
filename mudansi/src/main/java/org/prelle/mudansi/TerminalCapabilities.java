package org.prelle.mudansi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.DeviceAttributes.OperatingLevel;
import org.prelle.ansi.DeviceAttributes.TerminalType;
import org.prelle.ansi.DeviceAttributes.VT220Parameter;
import org.prelle.ansi.commands.SelectGraphicRendition;
import org.prelle.ansi.commands.SelectGraphicRendition.Meaning;

/**
 *
 */
public class TerminalCapabilities {

	OperatingLevel operatingLevel;
	List<VT220Parameter> features = new ArrayList<>();
	TerminalType generalCompatibility;
	String terminalName;
	// Layout capabilities
	boolean cursorPositioning;
	boolean editRectangular;
	boolean marginTopBottom;
	boolean marginLeftRight;
	// Image capabilities
	boolean inlineImageSixel;
	boolean inlineImageKitty;
	boolean inlineImageITerm;
	boolean color16;
	boolean color256;
	boolean color16m;
	int cellWidth, cellHeight;
	int screenWidth, screenHeight;

	//-------------------------------------------------------------------
	/**
	 */
	public TerminalCapabilities() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	private void outputOption(ANSIOutputStream out, String name, int width, boolean result) throws IOException {
		out.write(String.format("* %-"+width+"s", name));
		if (result) {
			out.write(new SelectGraphicRendition(List.of(Meaning.FOREGROUND_GREEN)));
			out.write("YES");
		} else {
			out.write(new SelectGraphicRendition(List.of(Meaning.FOREGROUND_RED)));
			out.write("NO");
		}
		out.write(new SelectGraphicRendition(List.of(Meaning.RESET)));
		out.write("\r\n");
	}

	public void report(ANSIOutputStream out) throws IOException {
		out.write(new SelectGraphicRendition(List.of(Meaning.RESET)));
		out.write("\r\n\n");
		// Primary DA
		if (operatingLevel==null) {
			out.write("Your terminal does not report operation level via Device Attributes\r\n");
		} else {
			out.write( String.format("Your terminal operates as a %s\r\n", operatingLevel));
			out.write( String.format("Your terminal feature set is %s\r\n", String.join(", ", features.stream().map(fe -> fe.name()).toList())));
		}
		// Secondary DA
		if (generalCompatibility==null) {
			out.write("Your terminal does NOT identify via Device Attributes 2\r\n");
		} else {
			out.write( String.format("Your terminal claims to be a %s\r\n", generalCompatibility));
		}
		// Terminal name
		if (terminalName==null) {
			out.write("Your terminal does NOT give a terminal name\r\n");
		} else {
			out.write( String.format("Your terminal names itself %s\r\n", terminalName));
		}
		// Cursor positioning
		out.write( String.format("Positioning the cursor anywhere %s supported\r\n", cursorPositioning?"is":"is NOT"));
		// Margins
		out.write( String.format("Splitting the area in top/bottom %s supported\r\n", marginTopBottom?"is":"is NOT"));
		out.write( String.format("Splitting the area in left/right %s supported\r\n", marginLeftRight?"is":"is NOT"));
		// Inline imaging
		out.write(new SelectGraphicRendition(List.of(Meaning.UNDERLINE_ON, Meaning.BOLD_ON)));
		out.write( "Inline imaging");
		out.write(new SelectGraphicRendition(List.of(Meaning.UNDERLINE_OFF, Meaning.RESET)));
		out.write( "\r\n");
		outputOption(out,"Sixel Graphics",20,inlineImageSixel);
		outputOption(out,"KiTTY Graphics",20,inlineImageKitty);
		outputOption(out,"iTerm Graphics",20,inlineImageITerm);
		out.write("- with a cell size of "+cellWidth+"x"+cellHeight);
		out.write( "\r\n");

	}

	//-------------------------------------------------------------------
	/**
	 * @return the operatingLevel
	 */
	public OperatingLevel getOperatingLevel() {
		return operatingLevel;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the features
	 */
	public List<VT220Parameter> getFeatures() {
		return features;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the generalCompatibility
	 */
	public TerminalType getGeneralCompatibility() {
		return generalCompatibility;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the cursorPositioning
	 */
	public boolean isCursorPositioning() {
		return cursorPositioning;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the editRectangular
	 */
	public boolean isEditRectangular() {
		return editRectangular;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the marginTopBottom
	 */
	public boolean isMarginTopBottom() {
		return marginTopBottom;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the marginLeftRight
	 */
	public boolean isMarginLeftRight() {
		return marginLeftRight;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the inlineImageSixel
	 */
	public boolean isInlineImageSixel() {
		return inlineImageSixel;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the inlineImageKitty
	 */
	public boolean isInlineImageKitty() {
		return inlineImageKitty;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the inlineITerm
	 */
	public boolean isInlineImageITerm() {
		return inlineImageITerm;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the terminalName
	 */
	public String getTerminalName() {
		return terminalName;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the color16
	 */
	public boolean isColor16() {
		return color16;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the color256
	 */
	public boolean isColor256() {
		return color256;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the color16m
	 */
	public boolean isColor16m() {
		return color16m;
	}

	//-------------------------------------------------------------------
	public int[] getScreenSize() {
		return new int[] {screenWidth, screenHeight};
	}

	//-------------------------------------------------------------------
	public int[] getCellSize() {
		return new int[] {cellWidth, cellHeight};
	}
	
}
