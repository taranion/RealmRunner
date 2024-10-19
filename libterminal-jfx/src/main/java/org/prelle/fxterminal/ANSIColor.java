package org.prelle.fxterminal;

/**
 * @see https://strasis.com/documentation/limelight-xe/reference/ecma-48-sgr-codes
 * @see https://en.wikipedia.org/wiki/ANSI_escape_code#3-bit_and_4-bit
 */
public class ANSIColor {

	public static String[] COLORS = new String[256];
	
	static {
		COLORS[0] = "#000000";
		COLORS[1] = "#800000";
		COLORS[2] = "#008000";
		COLORS[3] = "#808000";
		COLORS[4] = "#000080";
		COLORS[5] = "#800080";
		COLORS[6] = "#008080";
		COLORS[7] = "#c0c0c0";
		COLORS[8] = "#808080";
		COLORS[9] = "#ff0000";
		COLORS[10] = "#00ff00";
		COLORS[11] = "#ffff00";
		COLORS[12] = "#0000ff";
		COLORS[13] = "#ff00ff";
		COLORS[14] = "#00ffff";
		COLORS[15] = "#ffffff";

		String[] intensity = new String[] {"00","5f","87","af","d7","ff"};
		for (int x=0; x<intensity.length; x++) {
			for (int y=0; y<intensity.length; y++) {
				for (int z=0; z<intensity.length; z++) {
					String hex = "#"+intensity[x]+intensity[y]+intensity[z];
					int code = 16+x*36 + y*6 + z;
					//System.out.println(code+" = "+hex);
					COLORS[code] = hex;
				}

			}
			
		}
		COLORS[232] = "#080808";
		COLORS[233] = "#121212";
		COLORS[234] = "#1c1c1c";
		COLORS[235] = "#262626";
		COLORS[236] = "#303030";
		COLORS[237] = "#3a3a3a";
		COLORS[238] = "#404040";
		COLORS[239] = "#4e4e4e";
		COLORS[240] = "#585858";
		COLORS[241] = "#626262";
		COLORS[242] = "#6c6c6c";
		COLORS[243] = "#767676";
		COLORS[244] = "#808080";
		COLORS[245] = "#8a8a8a";
		COLORS[246] = "#949494";
		COLORS[247] = "#9e9e9e";
		COLORS[248] = "#a8a8a8";
		COLORS[249] = "#b2b2b2";
		COLORS[250] = "#bcbcbc";
		COLORS[251] = "#c6c6c6";
		COLORS[252] = "#d0d0d0";
		COLORS[253] = "#dadada";
		COLORS[254] = "#e4e4e4";
		COLORS[255] = "#eeeeee";

	}
}
