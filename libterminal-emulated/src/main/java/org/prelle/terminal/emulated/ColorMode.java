package org.prelle.terminal.emulated;

/**
 *
 */
public enum ColorMode {

	/** No colors */
	MONOCHROME,
	/** 8 Colors */
	BIT3,
	/** 16 Colors (8 regular +8 lighter variants) */
	BIT4,
	/** 16 Colors + 216 + 24 Grey levels */
	BIT8,
	/** Supports 3x8 Bit RGB values */
	TRUE_COLOR

}
