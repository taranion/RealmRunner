package org.prelle.mudansi;

import lombok.Setter;

/**
 * 
 */
public class EnergyBar {
	
	public static enum Color {
		RED(1),
		GREEN(2),
		YELLOW(3),
		BLUE(4),
		MAGENTA(5),
		CYAN(6)
		;
		int val=0;
		Color(int value) { val=value;}
		public String fore() { return "3"+val;}
		public String back() { return "4"+val;}
		public String brightFore() { return "9"+val;}
		public String brightBack() { return "10"+val;}
	}
	
	private Color color;
	
	@Setter private int max;
	@Setter private int current;
	@Setter private int occupied;

//	public static void main(String[] args) {
//		EnergyBar bar = new EnergyBar(Color.CYAN);
//		bar.setMax(30);
//		bar.setCurrent(20);
//		bar.setOccupied(2);
//		System.out.println( bar.renderUTF8(18) );
//		System.out.println( bar.renderASCII(18) );
//		System.out.println("123456789012345678");
//	}
	
	
	//-------------------------------------------------------------------
	public EnergyBar(Color color) {
		this.color = color;
	}

	//-------------------------------------------------------------------
	public String renderUTF8(int width) {
		StringBuffer buf = new StringBuffer();
		
		double pixelWidth = width*8;
//		System.out.println(max+"  ==> "+pixelWidth+" pixel");
		double goodPixel = Math.round( pixelWidth/((double)max) * current );
		double occuPixel = Math.round( pixelWidth/((double)max) * occupied );
//		System.out.println(current+"  ==> "+goodPixel+" pixel");
//		System.out.println(occupied+"  ==> "+occuPixel+" pixel");
		int fullFields = (int)(goodPixel/8);
		int occuFields = (int)(occuPixel/8);
		int partial    = (int)(goodPixel%8);
		int occuPartial= (int)(occuPixel%8);
		int emptyFields= width - fullFields - ((partial>0)?1:0) - occuFields- ((occuPartial>0)?1:0);
		if (emptyFields>1 && occuPartial>0) {
			emptyFields--;
		}
//		System.out.println(fullFields+" full, "+partial+" partial bits, "+emptyFields+" empty, "+occuPartial+" oc.partial, "+occuFields+" occupied");
//		System.out.println(occuFields+" occupied, "+occuPartial+" partial bits, "+emptyFields+" empty");
		char partialChar = (char)('\u2588'+(7-partial));
		char occuPartialChar = (char)('\u2588'+(7-occuPartial));
//		System.out.println("partial char = "+partialChar+"  "+Integer.toHexString(partialChar));
		buf.append("\u001B["+color.fore()+";"+color.back()+"m");
		buf.append("\u2588".repeat(fullFields));
		buf.append("\u001B["+color.fore()+";"+color.brightBack()+"m");
		buf.append(partialChar);
		buf.append("\u001B["+color.brightFore()+"m");
		buf.append("\u2588".repeat(emptyFields));
		if (emptyFields>1 && occuPartial>0) {
			buf.append("\u001B["+color.brightFore()+";47m");
			buf.append(occuPartialChar);
		}
		buf.append("\u001B[37;47m");
		buf.append("\u2588".repeat(occuFields));
		buf.append("\u001B[0m");
		
		return buf.toString();
	}

	//-------------------------------------------------------------------
	public String renderASCII(int width) {
		StringBuffer buf = new StringBuffer();
		
		double pixelWidth = width*8;
//		System.out.println(max+"  ==> "+pixelWidth+" pixel");
		double goodPixel = Math.round( pixelWidth/((double)max) * current );
		double occuPixel = Math.round( pixelWidth/((double)max) * occupied );
//		System.out.println(current+"  ==> "+goodPixel+" pixel");
//		System.out.println(occupied+"  ==> "+occuPixel+" pixel");
		int fullFields = (int)(goodPixel/8);
		int occuFields = (int)(occuPixel/8);
		int emptyFields= width - fullFields - occuFields;
//		System.out.println(fullFields+" full, "+partial+" partial bits, "+emptyFields+" empty, "+occuPartial+" oc.partial, "+occuFields+" occupied");
//		System.out.println(occuFields+" occupied, "+occuPartial+" partial bits, "+emptyFields+" empty");
//		System.out.println("partial char = "+partialChar+"  "+Integer.toHexString(partialChar));
		buf.append("\u001B["+color.back()+"m");
		buf.append(" ".repeat(fullFields));
		buf.append("\u001B["+color.brightBack()+"m");
		buf.append(" ".repeat(emptyFields));
		if (occuFields>0) {
			buf.append("\u001B[47m");
			buf.append(" ".repeat(occuFields));
		}
		buf.append("\u001B[0m");
		
		return buf.toString();
	}

}
