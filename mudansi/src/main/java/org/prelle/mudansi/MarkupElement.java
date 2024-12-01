package org.prelle.mudansi;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MarkupElement {

	private MarkupType type;
	private boolean endsMarkup;
	private String text;
	private int spacingWeight;
	private MarkupElement previousText;
	private Map<String,String> attributes = new HashMap<String, String>();

	//-------------------------------------------------------------------
	public MarkupElement(MarkupType type) {
		this.type = type;
	}

	//-------------------------------------------------------------------
	public MarkupElement(MarkupType type, String text) {
		this.type = type;
		this.text = text;
		if (text.endsWith("."))
			spacingWeight=4;
		else if (text.endsWith(";") || text.endsWith(":") || text.endsWith("-"))
			spacingWeight=3;
		else if (text.endsWith(",") )
			spacingWeight=2;
		else
			spacingWeight=1;
	}

	//-------------------------------------------------------------------
	public MarkupElement(MarkupType type, String text, boolean ended) {
		this.type = type;
		this.text = text;
		this.endsMarkup = ended;
	}

	//-------------------------------------------------------------------
	public MarkupElement(MarkupElement previousText) {
		this.type = MarkupType.SPACING;
		this.previousText = previousText;
	}

	//-------------------------------------------------------------------
	public String toString() {
		return type+":\""+text+"\":"+attributes;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the endsMarkup
	 */
	public boolean isEndsMarkup() {
		return endsMarkup;
	}

	//-------------------------------------------------------------------
	/**
	 * @param endsMarkup the endsMarkup to set
	 */
	public void setEndsMarkup(boolean endsMarkup) {
		this.endsMarkup = endsMarkup;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the type
	 */
	public MarkupType getType() {
		return type;
	}

	//-------------------------------------------------------------------
	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	//-------------------------------------------------------------------
	public int getLength() {
		if (type!=MarkupType.TEXT) return 0;
		return (text==null)?0:text.length();
	}

	//-------------------------------------------------------------------
	/**
	 * @return the spacingWeight
	 */
	public int getSpacingWeight() {
		if (type==MarkupType.SPACING && previousText!=null)
			return previousText.getSpacingWeight();
		return spacingWeight;
	}

	//-------------------------------------------------------------------
	/**
	 * @param spacingWeight the spacingWeight to set
	 */
	public void setSpacingWeight(int spacingWeight) {
		this.spacingWeight = spacingWeight;
	}

	//-------------------------------------------------------------------
	public void setAttribute(String key, String value) {
		if (value.charAt(0)==8)
			throw new IllegalArgumentException();
		this.attributes.put(key, value);
	}

	//-------------------------------------------------------------------
	public Map<String,String> getAttributes() { return this.attributes; }
	public String getAttribute(String key) { return this.attributes.get(key); }
	public int getAttributeAsInt(String key) { 
		if (!attributes.containsKey(key)) return 0;
		return Integer.parseInt(getAttribute(key)); 
	}

}
