package org.prelle.realmrunner.network;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.prelle.ansi.AParsedElement;

/**
 * 
 */
public class MXPStartTag extends AParsedElement {
	
	private static enum ParseMode {
		IDLE,
		KEY,
		EXPECT_SEPARATOR,
		EXPECT_VALUE,
		VALUE
	}
	
	private String name;
	
	private Map<String,String> parameter = new HashMap<>();

	//-------------------------------------------------------------------
	/**
	 */
	public MXPStartTag(String line) {
		StringTokenizer tok = new StringTokenizer(line);
		name = tok.nextToken();
		
		int i = name.length()+1;
		StringBuilder keyBuffer = new StringBuilder();
		StringBuilder valueBuffer = new StringBuilder();
		ParseMode mode = ParseMode.IDLE;
		for (;i<line.length(); i++) {
			char c = line.charAt(i);
			switch (mode) {
			case IDLE:
				if (Character.isWhitespace(c))
					continue;
				mode=ParseMode.KEY;
				keyBuffer.append(c);
				break;
			case KEY:
				if (Character.isWhitespace(c)) {
					mode=ParseMode.EXPECT_SEPARATOR;
					continue;
				}
				if (c=='=') {
					mode=ParseMode.EXPECT_VALUE;
					continue;
				}
				keyBuffer.append(c);
				break;
			case EXPECT_SEPARATOR:
				if (c=='=') {
					mode=ParseMode.EXPECT_VALUE;
					continue;
				} else if (!Character.isWhitespace(c)) {
					// Somethink like <element attr1 attr2=""> ... attr1 without value (assume true)
					parameter.put(keyBuffer.toString(), "true");
					keyBuffer.delete(0, keyBuffer.length());
					valueBuffer.delete(0, keyBuffer.length());
					mode=ParseMode.KEY;
					keyBuffer.append(c);
				}
				break;
			case EXPECT_VALUE:
				// 
				if (!Character.isWhitespace(c) && c!='"') {
					valueBuffer.append(c);
					mode=ParseMode.VALUE;
				}
				break;
			case VALUE:
				// 
				if (!Character.isWhitespace(c) && c!='"') {
					valueBuffer.append(c);
				} else {
					parameter.put(keyBuffer.toString(), valueBuffer.toString());
					keyBuffer.delete(0, keyBuffer.length());
					valueBuffer.delete(0, valueBuffer.length());
					mode=ParseMode.IDLE;
				}
				break;
			}
		}
		if (keyBuffer.length()>0) {
			parameter.put(keyBuffer.toString(), (valueBuffer.isEmpty())?"truer":valueBuffer.toString());
		}
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
	public Map<String,String> getAttributes() {
		return parameter;
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
