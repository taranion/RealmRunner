package org.prelle.realmrunner.network;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

import org.prelle.ansi.AParsedElement;

/**
 * 
 */
public class MXPEndTag extends AParsedElement {
	
	private String name;

	//-------------------------------------------------------------------
	/**
	 */
	public MXPEndTag(String line) {
		StringTokenizer tok = new StringTokenizer(line);
		name = tok.nextToken();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.ansi.AParsedElement#getName()
	 */
	@Override
	public String getName() {
		return "MXP:"+name;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.ansi.AParsedElement#encode(java.io.ByteArrayOutputStream, boolean)
	 */
	@Override
	public void encode(ByteArrayOutputStream toFill, boolean use7Bit) {
		// TODO Auto-generated method stub

	}

}
