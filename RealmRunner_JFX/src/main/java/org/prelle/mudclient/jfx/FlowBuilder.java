package org.prelle.mudclient.jfx;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import pk.ansi4j.core.DefaultFunctionFinder;
import pk.ansi4j.core.DefaultParserFactory;
import pk.ansi4j.core.DefaultTextHandler;
import pk.ansi4j.core.api.Environment;
import pk.ansi4j.core.api.Fragment;
import pk.ansi4j.core.api.FragmentType;
import pk.ansi4j.core.api.FunctionFragment;
import pk.ansi4j.core.api.ParserFactory;
import pk.ansi4j.core.api.StringParser;
import pk.ansi4j.core.api.TextFragment;
import pk.ansi4j.core.api.function.FunctionArgument;
import pk.ansi4j.core.api.iso6429.C0ControlFunction;
import pk.ansi4j.core.api.iso6429.ControlSequenceFunction;
import pk.ansi4j.core.iso6429.C0ControlFunctionHandler;
import pk.ansi4j.core.iso6429.C1ControlFunctionHandler;
import pk.ansi4j.core.iso6429.ControlSequenceHandler;
import pk.ansi4j.core.iso6429.ControlStringHandler;
import pk.ansi4j.core.iso6429.IndependentControlFunctionHandler;

/**
 *
 */
public class FlowBuilder {

	private enum ColorMode {
		NORMAL,
		IN_38,
		IN_38_2, // True Color
		IN_38_5, // 256 Color
		IN_48,
		IN_48_2, // True Color
		IN_48_5
	}

	private final static Color BLACK = Color.rgb(0, 0, 0);
	private final static Color RED   = Color.rgb(128, 0, 0);
	private final static Color GREEN = Color.valueOf("#008000");
	private final static Color OLIVE = Color.valueOf("#808000");
	private final static Color NAVY  = Color.valueOf("#000080");
	private final static Color PURPLE= Color.valueOf("#800080");
	private final static Color CYAN  = Color.valueOf("#008080");
	private final static Color SILVER= Color.valueOf("#C0C0C0");
	private final static Color BRIGHT_BLACK = Color.rgb(128, 128, 128);
	private final static Color BRIGHT_RED   = Color.valueOf("#FF0000");
	private final static Color BRIGHT_GREEN = Color.valueOf("#00FF00");
	private final static Color BRIGHT_OLIVE = Color.valueOf("#FFFF00");
	private final static Color BRIGHT_NAVY  = Color.valueOf("#0000FF");
	private final static Color BRIGHT_PURPLE= Color.valueOf("#FF00FF");
	private final static Color BRIGHT_CYAN  = Color.valueOf("#00FFFF");
	private final static Color BRIGHT_SILVER= Color.valueOf("#FFFFFF");

	private final static Logger logger = System.getLogger("mud.jfx");


	private int fontSize = 10;
	private String fontFamily = "Monospaced Regular";
	private String message = "Lorem Ipsum";
	private boolean darkMode = true;

	private ParserFactory factory = new DefaultParserFactory.Builder()
	        .environment(Environment._7_BIT)
	        .textHandler(new DefaultTextHandler())
	        .functionFinder(new DefaultFunctionFinder())
	        //if you don't need some types of functions just don't provide handlers for them
	        .functionHandlers(
	                new C0ControlFunctionHandler(),
	                new C1ControlFunctionHandler(),
	                new ControlSequenceHandler(),
	                new IndependentControlFunctionHandler(),
	                new ControlStringHandler())
	        .build();

	//-------------------------------------------------------------------
	public static FlowBuilder configure() {
		logger.log(Level.WARNING, "Font families = "+ Font.getFamilies());
		return new FlowBuilder();
	}

	//-------------------------------------------------------------------
	public FlowBuilder darkMode(boolean data) {
		this.darkMode=data;
		return this;
	}

	//-------------------------------------------------------------------
	public FlowBuilder fontSize(int data) {
		this.fontSize=data;
		return this;
	}

	//-------------------------------------------------------------------
	public FlowBuilder fontFamily(String name) {
		this.fontFamily=name;
		return this;
	}

	//-------------------------------------------------------------------
	public FlowBuilder message(String data) {
		this.message=data;
		return this;
	}

	private static String colorToHex(Color c) {
		return String.format( "#%02X%02X%02X",
	            (int)( c.getRed() * 255 ),
	            (int)( c.getGreen() * 255 ),
	            (int)( c.getBlue() * 255 ) );
	}

	//-------------------------------------------------------------------
	public Node build() {
		VBox flows = new VBox(0);
		Color defaultForeground = Color.valueOf("#E0E0E0");
		Color defaultBackground = Color.valueOf("black");
		Font defaultFont = new Font(fontFamily, fontSize);
		logger.log(Level.INFO, "Chose font "+defaultFont);
		if (darkMode) {
			flows.setStyle("-fx-background-color: black");
		} else {
			defaultForeground = Color.valueOf("#000000");
			flows.setStyle("-fx-background-color: #F0F0F0");
			defaultBackground = Color.valueOf("#F0F0F0");
		}

		Font font = defaultFont;
		Color foreground = defaultForeground;
		Color background = defaultBackground;
		FlowPane flow = createFlow();
		flows.getChildren().add(flow);

		StringParser parser = factory.createParser(message);
		Fragment fragment = null;
		while ((fragment = parser.parse()) != null) {
		    if (fragment.getType() == FragmentType.TEXT) {
		        TextFragment textFragment = (TextFragment) fragment;
//		        logger.log(Level.INFO, "TEXT: "+textFragment.getText());
//		        Label text = new Label(textFragment.getText());
				Text text = new Text(textFragment.getText());
				text.setFont(font);
				text.setFill(foreground);
				text.setLineSpacing(0);
				text.setBoundsType(TextBoundsType.LOGICAL);
				text.setUnderline(false);
				//text.setTextFill(foreground);
//				text.setBackground(new BackG);
				text.setStyle("-fx-background-color: "+colorToHex(background));

				StackPane stack = new StackPane(text);
				stack.setBackground(Background.fill(background));
				stack.layoutBoundsProperty().addListener( (ov,o,n) -> {
//					logger.log(Level.INFO,"Layout of "+textFragment.getText().length()+" is "+n);
				});
				flow.getChildren().add(stack);

		    } else if (fragment.getType() == FragmentType.FUNCTION) {
		        FunctionFragment functionFragment = (FunctionFragment) fragment;
		        if (functionFragment.getFunction() == ControlSequenceFunction.SGR_SELECT_GRAPHIC_RENDITION) {
		        	ColorMode mode = ColorMode.NORMAL;
			        for (FunctionArgument arg : functionFragment.getArguments()) {
			        	switch (mode) {
			        	case NORMAL:
			        	switch ((Integer)arg.getValue()) {
			        	case 0:
			        		// Reset
			        		font = defaultFont;
			        		foreground = BLACK;
			        		break;
			        	case 1:
			        		// Bold
			        		font = Font.font(fontFamily, FontWeight.BOLD, fontSize);
			        		break;
			        	case 30: foreground = BLACK; break;
			        	case 31: foreground = RED; break;
			        	case 32: foreground = GREEN; break;
			        	case 33: foreground = OLIVE; break;
			        	case 34: foreground = NAVY; break;
			        	case 35: foreground = PURPLE; break;
			        	case 36: foreground = CYAN; break;
			        	case 37: foreground = SILVER; break;
			        	case 38: mode = ColorMode.IN_38; break;
			        	case 39: foreground = defaultForeground; break;
			        	case 40: background = BLACK; break;
			        	case 41: background = RED; break;
			        	case 42: background = GREEN; break;
			        	case 43: background = OLIVE; break;
			        	case 44: background = NAVY; break;
			        	case 45: background = PURPLE; break;
			        	case 46: background = CYAN; break;
			        	case 47: background = SILVER; break;
			        	case 48: mode = ColorMode.IN_48; break;
			        	case 49: background = defaultBackground; break;
			        	case 90: foreground = BRIGHT_BLACK; break;
			        	case 91: foreground = BRIGHT_RED; break;
			        	case 92: foreground = BRIGHT_GREEN; break;
			        	case 93: foreground = BRIGHT_OLIVE; break;
			        	case 94: foreground = BRIGHT_NAVY; break;
			        	case 95: foreground = BRIGHT_PURPLE; break;
			        	case 96: foreground = BRIGHT_CYAN; break;
			        	case 97: foreground = BRIGHT_SILVER; break;
			        	default:
					        logger.log(Level.INFO, "SGR: "+functionFragment.getArguments().stream().map(a -> a.getValue()).toList());
			        	}
			        		break;
			        	case IN_38:
				        	switch ((Integer)arg.getValue()) {
				        	case 2: // True Color
				        		mode = ColorMode.IN_38_2;
				        		break;
				        	case 5: // 256 color
				        		mode = ColorMode.IN_38_5;
				        		break;
				        	default:
				        		logger.log(Level.INFO, "TODO: process {0} in mode {1}",arg.getValue(),mode);
				        	}
				        	break;
			        	case IN_38_5:
			        		int ansi = (Integer)arg.getValue();
			        		if (ANSIColor.COLORS[ansi]!=null) {
			        			foreground = Color.valueOf(ANSIColor.COLORS[ansi]);
			        		} else {
						        logger.log(Level.INFO, "Undefined ANSI color {0}",ansi);
				        	}
			        		mode = ColorMode.NORMAL;

				        	break;
			        	case IN_48:
				        	switch ((Integer)arg.getValue()) {
				        	case 2: // True Color
				        		mode = ColorMode.IN_48_2;
				        		break;
				        	case 5: // 256 color
				        		mode = ColorMode.IN_48_5;
				        		break;
				        	default:
				        		logger.log(Level.INFO, "TODO: process {0} in mode {1}",arg.getValue(),mode);
				        	}
				        	break;
			        	case IN_48_5:
			        		ansi = (Integer)arg.getValue();
			        		if (ANSIColor.COLORS[ansi]!=null) {
			        			background = Color.valueOf(ANSIColor.COLORS[ansi]);
			        		} else {
						        logger.log(Level.INFO, "Undefined ANSI color {0}",ansi);
				        	}
			        		mode = ColorMode.NORMAL;

				        	break;
			        	default:
			        		logger.log(Level.INFO, "TODO: in mode "+mode);
			        	}
			        }
		        } else if (functionFragment.getFunction() == C0ControlFunction.LF_LINE_FEED) {
//		        	Region p = new Region();
//		        	p.setPrefSize(Double.MAX_VALUE, 0.0);
//		        	flow.getChildren().add(p);
		        	flow = createFlow();
		        	flows.getChildren().add(flow);
		        } else {
			        logger.log(Level.INFO, "FUNCTION: "+functionFragment.getFunction()+": "+functionFragment.getArguments());
		        }
		    }
		}

//		TerminalDecoder terminal = new TerminalDecoder(message);
//		while (true) {
//			TerminalEvent ev = terminal.read();
//			if (ev==null)
//				break;
//			logger.log(Level.INFO, "RCV "+ev);
//		}

		return flows;
	}

	private FlowPane createFlow() {
		FlowPane flow = new FlowPane();
		flow.setVgap(0);
		flow.setHgap(0);
    	return flow;
	}
}
