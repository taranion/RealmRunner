package org.prelle.mudansi;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.prelle.ansi.commands.SelectGraphicRendition;
import org.prelle.ansi.commands.SelectGraphicRendition.Meaning;

/**
 *
 */
public class FormatUtil {

    public final static Logger LOGGER = System.getLogger(FormatUtil.class.getPackageName());

    //-------------------------------------------------------------------
    public static String convertTextBlock(List<MarkupElement> markup, int lineLength) {
        return String.join("\r\n", convertText(markup, lineLength));
    }

    //-------------------------------------------------------------------
    public static List<String> convertText(List<MarkupElement> markup, int lineLength) {
        List<String> buf = new ArrayList<>();
        LOGGER.log(Level.TRACE, "Convert to width {0}", lineLength);
        if (markup==null) {
        	LOGGER.log(Level.WARNING, "convertText() called with NULL");
        	return buf;
        }

        List<MarkupElement> currentLine = new ArrayList<>();
        for (MarkupElement tmp : markup) {
//			LOGGER.log(Level.INFO, tmp);
            MarkupElement last = (currentLine.isEmpty()) ? null : currentLine.getLast();
            if (last != null && last.getType() == MarkupType.TEXT) {
                currentLine.add(new MarkupElement(last));
            }

            switch (tmp.getType()) {
                case TEXT:
                    List<MarkupElement> textElements = currentLine.stream().filter(e -> e.getType() == MarkupType.TEXT).toList();
                    int currentTextLength = (int) textElements.stream()
                            .map(elem -> elem.getLength())
                            .reduce(0, Integer::sum);
                    // Minimal spaces
                    currentTextLength += textElements.size();
                    // Will it fit in the line?
                    if ((currentTextLength + tmp.getLength()) <= lineLength) {
                        // Fits
                        currentLine.add(tmp);
                    } else {
                        // Won't fit
                        // Existing line will be written
//                    	LOGGER.log(Level.WARNING, "Wont fit: {0} + {1} > {2}", currentTextLength, tmp.getLength(), lineLength);
                        buf.add(buildLine(currentLine, lineLength, false));
                        currentLine.clear();
                        currentLine.add(tmp);
                    }
                    break;
                case FLOW:
                    if (tmp.getText().equals("br")) {
                        buf.add(buildLine(currentLine, lineLength, false));
                        currentLine.clear();
                    }
                default:
                    currentLine.add(tmp);
            }
        }
        if (!currentLine.isEmpty()) {
//			logger.log(Level.INFO, "Remain "+currentLine);
            buf.add(buildLine(currentLine, lineLength, false));
        }

        return buf;
    }

    //-------------------------------------------------------------------
    public static String buildLine(List<MarkupElement> elements, int lineLength, boolean justify) {
        if (justify) {
            LOGGER.log(Level.WARNING, "TODO: Implement justification");
        }
        StringBuilder ret = new StringBuilder();
        StringBuilder span = new StringBuilder();
        MarkupElement spanElem = null;
        for (MarkupElement elem : elements) {
            switch (elem.getType()) {
                case TEXT:
                	if (spanElem==null) {
                		// Not in SPAN mode
                		ret.append(elem.getText());
                	} else {
                		span.append(elem.getText());
                	}
                    break;
                case SPACING:
                	// Add space if there wasn't one just before (due to PRE)
                	if (ret.length()==0 || ret.charAt(ret.length()-1)!=' ') {
                    	if (spanElem==null) {
                    		// Not in SPAN mode
                    		ret.append(' ');
                    	} else {
                    		span.append(' ');
                    	}
                	}
                    break;
                case STYLE:
                case COLOR:
                    if (elem.isEndsMarkup()) {
                        tagEnded(elem.getText(), (spanElem!=null)?span:ret);
                    } else {
                        tagStarted(elem.getText(), (spanElem!=null)?span:ret);
                    }
                    break;
//			case ENTITY:
//				if ("nbsp".equals(elem.getText())) {
//
//				}
//				ret.append(' '); break;
                case FLOW:
                    if (elem.getText().equalsIgnoreCase("br")) {
//					ret.append("\r\n");
                        continue;
                    } else if (elem.getText().equalsIgnoreCase("PRE")) {
                    	continue;
                    } else if (elem.getText().equalsIgnoreCase("SPAN")) {
                    	if (elem.isEndsMarkup()) {
                    		int width = (spanElem!=null)? spanElem.getAttributeAsInt("width"):0;
                    		if (width>0) {
                    			ret.append(String.format("%-"+width+"s", span.toString()));
                    			span.delete(0, span.length());
                    		} else {
                    			ret.append(span.toString());
                    		}
                    		spanElem = null;
                    	} else {
                    		spanElem = elem;
                    	}
                    	continue;
                    } else {
                    	LOGGER.log(Level.WARNING, "Unsupported flow "+elem.getText());
                    }
                default:
                    LOGGER.log(Level.WARNING, "No output for " + elem);
            }
        }
        return ret.toString();
    }

    //-------------------------------------------------------------------
    public static void tagStarted(String tagName, StringBuilder buf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SelectGraphicRendition sgr = null;
        switch (tagName) {
            case "b":
                sgr = new SelectGraphicRendition(List.of(Meaning.BOLD_ON));
                break;
            case "blink":
                sgr = new SelectGraphicRendition(List.of(Meaning.BLINKING_ON));
                break;
            case "blue":
                sgr = new SelectGraphicRendition((List.of(Meaning.FOREGROUND_BRIGHT_BLUE)));
                break;
            case "cyan":
                sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_CYAN));
                break;
            case "f":
                sgr = new SelectGraphicRendition(List.of(Meaning.FAINT_ON));
                break;
            case "green":
                sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_GREEN));
                break;
            case "i":
                sgr = new SelectGraphicRendition(List.of(Meaning.ITALIC_ON));
                break;
            case "n":
                sgr = new SelectGraphicRendition(List.of(Meaning.NEGATIVE_ON));
                break;
            case "reset":
                sgr = new SelectGraphicRendition(List.of(Meaning.RESET));
                break;
            case "red":
                sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_RED));
                break;
            case "strike":
                sgr = new SelectGraphicRendition(List.of(Meaning.CROSSEDOUT_ON));
                break;
            case "u":
                sgr = new SelectGraphicRendition(List.of(Meaning.UNDERLINE_ON));
                break;
            case "yellow":
                sgr = new SelectGraphicRendition((List.of(Meaning.FOREGROUND_YELLOW)));
                break;
            default:
                LOGGER.log(Level.WARNING, "Unhandled tag {0}", tagName);
                System.err.println("Unhandled start tag " + tagName);
        }

        if (sgr != null) {
            sgr.encode(baos, true);
            buf.append(baos.toString(StandardCharsets.US_ASCII));
        }
    }

    //-------------------------------------------------------------------

    /**
     * @param tagName
     * @param buf
     * @return TRUE, if linebreak
     */
    public static boolean tagEnded(String tagName, StringBuilder buf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SelectGraphicRendition sgr = null;
        switch (tagName) {
            case "b":
                sgr = new SelectGraphicRendition(List.of(Meaning.INTENSITY_OFF));
                break;
            case "blink":
                sgr = new SelectGraphicRendition(List.of(Meaning.BLINK_OFF));
                break;
            case "br":
                buf.append("\n");
                return true;
            case "blue":
            case "yellow":
            case "cyan":
            case "green":
                sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_DEFAULT));
                break;
            case "f":
                sgr = new SelectGraphicRendition(List.of(Meaning.INTENSITY_OFF));
                break;
            case "i":
                sgr = new SelectGraphicRendition(List.of(Meaning.STYLE_OFF));
                break;
            case "n":
                sgr = new SelectGraphicRendition(List.of(Meaning.NEGATIVE_OFF));
                break;
            case "red":
                sgr = new SelectGraphicRendition(List.of(Meaning.FOREGROUND_DEFAULT));
                break;
            case "strike":
                sgr = new SelectGraphicRendition(List.of(Meaning.CROSSEDOUT_OFF));
                break;
            case "u":
                sgr = new SelectGraphicRendition(List.of(Meaning.UNDERLINE_OFF));
                break;
            default:
                LOGGER.log(Level.WARNING, "Unhandled tag {0}", tagName);
                System.err.println("Unhandled end tag " + tagName);
        }

        if (sgr != null) {
            sgr.encode(baos, true);
            buf.append(baos.toString(StandardCharsets.US_ASCII));
        }
        return false;
    }

}
