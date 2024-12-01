package test;

import java.util.List;

import org.prelle.mudansi.FormatUtil;
import org.prelle.mudansi.MarkupElement;
import org.prelle.mudansi.MarkupParser;

/**
 *
 */
public class MarkupParserTest {

	//-------------------------------------------------------------------
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String text = "<b><u>Tables before a stage</u></b> [ <cyan>E S</cyan> ]<br/>"+
				"A dozen tables are jammed before a small stage, which is elevated just enough to provide a clear view from any spot. A heavy curtain is drawn across the stage, but a spotlight shines on a single microphone dangling from somewhere overhead.";

		List<MarkupElement> markup = MarkupParser.convertText(text);
		for (MarkupElement elem : markup) {
			System.out.println(elem);
		}

		List<String> toSend = FormatUtil.convertText(markup, 70);
//		for (String line : toSend)
//			System.out.println(line);
		
//		text = "<pre>Hello     </pre>Da sollten jetzt 5 Spaces gewesen sein";
//		markup = MarkupParser.convertText(text);
		
		text = "<span width=\"10\">Hello</span>Da sollten jetzt 5 Spaces gewesen sein";
		markup = MarkupParser.convertText(text);
		for (MarkupElement elem : markup) {
			System.out.println(elem);
		}
		toSend = FormatUtil.convertText(markup, 70);
		for (String line : toSend)
			System.out.println(line);
	}

}
