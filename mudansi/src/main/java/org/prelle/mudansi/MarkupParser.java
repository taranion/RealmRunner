package org.prelle.mudansi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MarkupParser {

	private final static Logger logger = System.getLogger(MarkupParser.class.getPackageName());

	//-------------------------------------------------------------------
	private static enum ParseMode {
		TEXT,
		TAG_NAME,
		ATTRIBUTE_NAME,
		ATTRIBUTE_VALUE,
		ENTITY;
	}

	//-------------------------------------------------------------------
	public static List<MarkupElement> convertText(String raw) {
		List<MarkupElement> ret = new ArrayList<>();
		if (raw==null) return ret;
		raw = raw.replace("\r\n", "<br/>");
		raw = raw.replace("\n", "<br/>");		

		ParseMode mode = ParseMode.TEXT;
		StringBuffer tag = new StringBuffer();
		StringBuffer text = new StringBuffer();
		Character lastChar = null;
		MarkupElement currentElem = null;
		boolean inPRE = false;
		boolean wasEndTag = false;
		for (int i=0; i<raw.length(); i++) {
			char c = raw.charAt(i);
			switch (mode) {
			case TEXT:
				switch (c) {
				case '\n':
				case '\r':
					break;
				case '<':
					if (!text.isEmpty()) {
						ret.add(new MarkupElement(MarkupType.TEXT, inPRE?text.toString():text.toString().trim()));
						text.delete(0, text.length());
					}
					mode = ParseMode.TAG_NAME;
					tag.delete(0, tag.length());
					wasEndTag=false;
					currentElem=null;
					lastChar=null;
					break;
				case ' ':
					if (inPRE) {
						text.append(c);
					} else {
						if (!text.isEmpty()) {
							ret.add(new MarkupElement(MarkupType.TEXT, text.toString().trim()));
							text.delete(0, text.length());
						}
						lastChar=null;
					}
					break;
				case '&':
					mode = ParseMode.ENTITY;
					tag.delete(0, tag.length());
					lastChar=null;
					break;
				default:
					if (Character.isWhitespace(c)) {
						if (lastChar==null || !Character.isWhitespace(lastChar))
							text.append(c);
					} else {
						text.append(c);
					}
					lastChar=c;
				}
				break;
			case TAG_NAME:
				switch (c) {
				case '/':
					wasEndTag = true;
					break;
				case '>':
					if ("PRE".equalsIgnoreCase(tag.toString())) {
						inPRE = !wasEndTag;
					}
					mode = ParseMode.TEXT;
					MarkupElement elem = tag(tag.toString(), wasEndTag);
					currentElem = elem;
					if (elem!=null) {
						ret.add(elem);
					}
					tag.delete(0, tag.length());
					text.delete(0, text.length());
					break;
				case ' ':
					// Tag name ended, maybe an attribute starts
					mode = ParseMode.ATTRIBUTE_NAME;
					elem = tag(tag.toString(), wasEndTag);
					currentElem = elem;
					if (elem!=null) {
						ret.add(elem);
					}
					tag.delete(0, tag.length());
					text.delete(0, text.length());
					break;
				default:
					tag.append(c);
				}
				break;
			case ENTITY:
				switch (c) {
				case ';':
					mode = ParseMode.TEXT;
					MarkupElement elem = new MarkupElement(
							("nbsp".equals(tag.toString()))?MarkupType.SPACING:MarkupType.ENTITY,
									tag.toString()
									);
					ret.add(elem);
					tag.delete(0, tag.length());
					break;
				default:
					tag.append(c);
				}
				break;
			case ATTRIBUTE_NAME:
				switch (c) {
				case ' ':
					break;
				case '>':
					mode = ParseMode.TEXT;
					break;
				case '=':
					mode=ParseMode.ATTRIBUTE_VALUE;
				default:
					if (Character.isJavaIdentifierStart(c)) {
						tag.append(c);
					}
				}
				break;
			case ATTRIBUTE_VALUE:
				switch (c) {
				case ' ':
				case '\"':
				case '\\':
					break;
				case '>':
					mode = ParseMode.TEXT;
					if (currentElem!=null) {
						currentElem.setAttribute(tag.toString(), text.toString());
					}
					text.delete(0,  text.length());
					tag.delete(0, tag.length());
					break;
				default:
					text.append(c);
				}
				break;
			default:
				logger.log(Level.ERROR, "Unsupported mode "+mode);
			}
		}
		if (!text.isEmpty()) {
			ret.add(new MarkupElement(MarkupType.TEXT, text.toString().trim()));
			text.delete(0, text.length());
		}

		return ret;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudansi.FormatUtil.tagStarted(String,StringBuffer)
	 */
	private static MarkupElement tag(String tagName, boolean ended) {
		switch (tagName) {
		case "b"    : return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "black": return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "blink": return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "blue" : return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "br"   : return new MarkupElement(MarkupType.FLOW, tagName, ended);
		case "cyan" : return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "default": return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "f"    : return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "green": return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "i"    : return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "n"    : return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "pre"  : return new MarkupElement(MarkupType.FLOW,  tagName, ended);
		case "red"  : return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "reset": return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "sgr"  : return new MarkupElement(MarkupType.COLOR,  tagName, ended);
		case "span" : return new MarkupElement(MarkupType.FLOW,  tagName, ended);
		case "strike": return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "u"    : return new MarkupElement(MarkupType.STYLE, tagName, ended);
		case "white": return new MarkupElement(MarkupType.COLOR, tagName, ended);
		case "yellow": return new MarkupElement(MarkupType.COLOR, tagName, ended);
		default:
			logger.log(Level.WARNING, "Unhandled tag {0}", tagName);
		}
		return null;
	}

}
