package org.prelle.terminal.console;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;
/**
 * @see https://learn.microsoft.com/en-us/windows/console/high-level-console-modes
 */
public class WindowsConsoleFFM implements TerminalEmulator {
	
	private final static Logger logger = System.getLogger("terminal.windows");
	
	private static enum InputFlag {
		/**
		 * CTRL+C is processed by the system and is not placed in the input buffer. 
		 * If the input buffer is being read by ReadFile or ReadConsole, other 
		 * control keys are processed by the system and are not returned in the 
		 * ReadFile or ReadConsole buffer. If the ENABLE_LINE_INPUT mode is also 
		 * enabled, backspace, carriage return, and line feed characters are handled by the system.
		 */
		ENABLE_PROCESSED_INPUT(0x0001),
		/**
		 * The ReadFile or ReadConsole function returns only when a carriage return 
		 * character is read. If this mode is disabled, the functions return when 
		 * one or more characters are available.
		 */
		ENABLE_LINE_INPUT(0x0002),
		/**
		 * Characters read by the ReadFile or ReadConsole function are written 
		 * to the active screen buffer as they are typed into the console. 
		 * This mode can be used only if the ENABLE_LINE_INPUT mode is also enabled.
		 */
		ENABLE_ECHO_INPUT(0x0004),
		/**
		 * User interactions that change the size of the console screen buffer are 
		 * reported in the console's input buffer. Information about these events 
		 * can be read from the input buffer by applications using the ReadConsoleInput 
		 * function, but not by those using ReadFile or ReadConsole.
		 */
		ENABLE_WINDOW_INPUT(0x0008),
		/**
		 * If the mouse pointer is within the borders of the console window and the 
		 * window has the keyboard focus, mouse events generated by mouse movement 
		 * and button presses are placed in the input buffer. These events are 
		 * discarded by ReadFile or ReadConsole, even when this mode is enabled. 
		 * The ReadConsoleInput function can be used to read MOUSE_EVENT input records 
		 * from the input buffer.
		 */
		ENABLE_MOUSE_INPUT(0x0010),
		ENABLE_INSERT_MODE(0x0020),
		ENABLE_QUICK_EDIT_MODE(0x0040),
		ENABLE_EXTENDED_FLAGS(0x0080),
		/**
		 * My initial best guess is that it is related to the "Let system position window" check box on the property sheet of a CMD.EXE window, though I have yet to test this theory.
		 */
		ENABLE_AUTO_POSITION(0x0100),
		/**
		 * Setting this flag directs the Virtual Terminal processing engine to 
		 * convert user input received by the console window into Console Virtual 
		 * Terminal Sequences that can be retrieved by a supporting application 
		 * through ReadFile or ReadConsole functions.
		 *
		 * The typical usage of this flag is intended in conjunction with 
		 * ENABLE_VIRTUAL_TERMINAL_PROCESSING on the output handle to connect to an 
		 * application that communicates exclusively via virtual terminal sequences.
		 */
		ENABLE_VIRTUAL_TERMINAL_INPUT(0x0200)

		;
		int val;
		InputFlag(int val) {
			this.val = val;
		}
		public int value() { return val; }
	}

	
	private static enum OutputFlag {
		ENABLE_PROCESSED_OUTPUT(0x0001),
		ENABLE_WRAP_AT_EOL_OUTPUT(0x0002),
		ENABLE_VIRTUAL_TERMINAL_PROCESSING(0x0004),
		DISABLE_NEWLINE_AUTO_RETURN(0x0008)
		;
		int val;
		OutputFlag(int val) {
			this.val=val;
		}
		public int value() { return val; }
	}

    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
    // Standard-Handles
    private static final int STD_INPUT_HANDLE = -10;
    private static final int STD_OUTPUT_HANDLE = -11;

	private int savedStateIn, savedStateOut;
    private ANSIOutputStream out;
    private ANSIInputStream in;

    private Arena arena;
    private MethodHandle GetStdHandle, GetConsoleMode, SetConsoleMode, GetConsoleScreenBufferInfo;
    private MethodHandle getConsoleCP, getConsoleOutputCP, setConsoleCP, setConsoleOutputCP;
    private MemorySegment stdInHandle, stdOutHandle;
    private MemorySegment currentMode;
    private MemorySegment consoleInfo;
    private GroupLayout consoleScreenBufferInfoLayout;

	//-------------------------------------------------------------------
	/**
	 */
	public WindowsConsoleFFM() {
        arena = Arena.ofShared();
        
        // Define functions
    	GetStdHandle = linker.downcallHandle(
                kernel32.find("GetStdHandle").orElseThrow(() -> new IllegalStateException("GetStdHandle not found")),
                FunctionDescriptor.of(ADDRESS, JAVA_INT)
            );

    	GetConsoleMode = linker.downcallHandle(
    			kernel32.find("GetConsoleMode").orElseThrow(() -> new IllegalStateException("GetConsoleMode not found")),
    			FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
        );

        SetConsoleMode = linker.downcallHandle(
        		kernel32.find("SetConsoleMode").orElseThrow(() -> new IllegalStateException("SetConsoleMode not found")),
        		FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT)
        );
        GetConsoleScreenBufferInfo = Linker.nativeLinker().downcallHandle(
                kernel32.find("GetConsoleScreenBufferInfo").orElseThrow(() -> new IllegalStateException("GetConsoleScreenBufferInfo not found")),
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS)
            );
        getConsoleCP = Linker.nativeLinker().downcallHandle(
                kernel32.find("GetConsoleCP").orElseThrow(() -> new IllegalStateException("GetConsoleCP not found")),
                FunctionDescriptor.of(JAVA_INT)
            );
        getConsoleOutputCP = Linker.nativeLinker().downcallHandle(
                kernel32.find("GetConsoleOutputCP").orElseThrow(() -> new IllegalStateException("GetConsoleOutputCP not found")),
                FunctionDescriptor.of(JAVA_INT)
            );
        setConsoleOutputCP = Linker.nativeLinker().downcallHandle(
                kernel32.find("SetConsoleOutputCP").orElseThrow(() -> new IllegalStateException("SetConsoleOutputCP not found")),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT)
            );
        setConsoleCP = Linker.nativeLinker().downcallHandle(
                kernel32.find("SetConsoleCP").orElseThrow(() -> new IllegalStateException("SetConsoleCP not found")),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT)
        );
        
       	currentMode = arena.allocate(JAVA_INT);
        // Definition der CONSOLE_SCREEN_BUFFER_INFO-Struktur
        consoleScreenBufferInfoLayout = MemoryLayout.structLayout(
            JAVA_INT.withName("dwSizeX"),
            JAVA_INT.withName("dwSizeY"),
            JAVA_INT.withName("dwCursorPositionX"),
            JAVA_INT.withName("dwCursorPositionY"),
            JAVA_SHORT.withName("wAttributes"),
            JAVA_SHORT.withName("srWindowLeft"),
            JAVA_SHORT.withName("srWindowTop"),
            JAVA_SHORT.withName("srWindowRight"),
            JAVA_SHORT.withName("srWindowBottom"),
            JAVA_SHORT.withName("dwMaximumWindowSizeX"),
            JAVA_SHORT.withName("dwMaximumWindowSizeY")
        );
        consoleInfo = arena.allocate(consoleScreenBufferInfoLayout);

        out = new ANSIOutputStream(System.out);
        in  = new ANSIInputStream(System.in);
        
      	try {
			stdOutHandle = (MemorySegment) GetStdHandle.invokeExact(STD_OUTPUT_HANDLE);
			stdInHandle = (MemorySegment) GetStdHandle.invokeExact(STD_INPUT_HANDLE);
		} catch (Throwable e) {
			logger.log(Level.ERROR, "Error getting handles",e);
			System.exit(1);
		}
      	savedStateIn = getInputMode();
      	savedStateOut= getOutputMode();

        int originalMode = currentMode.get(JAVA_INT, 0);
        System.out.println("Ursprünglicher Modus: " + originalMode);
        debugModes();
        setANSICompatibility();
        // Set to UTF-8
        setInputCodepage(65001);
        setOutputCodepage(65001);
        getEncodings();

        Thread restoreHook = new Thread( () -> {
        	logger.log(Level.INFO, "Resetting cooked mode and local echo");
        	setMode(TerminalMode.LINE_MODE);
        	setLocalEchoActive(true);
        });
        Runtime.getRuntime().addShutdownHook(restoreHook);
	}

	private void setANSICompatibility() {
//		setInputFlags(InputFlag.ENABLE_VIRTUAL_TERMINAL_INPUT);
//		setOutputFlags(OutputFlag.ENABLE_VIRTUAL_TERMINAL_PROCESSING, OutputFlag.DISABLE_NEWLINE_AUTO_RETURN);		
	}

	//-------------------------------------------------------------------
	private int getInputMode() {
		try {
			int result = (int) GetConsoleMode.invokeExact(stdInHandle, currentMode);
			if (result == 0) {
			    throw new IllegalStateException("GetConsoleMode failed");
			}

			return currentMode.get(JAVA_INT, 0);
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
        	return 0;
		}
	}

	//-------------------------------------------------------------------
	private int getOutputMode() {
		try {
			int result = (int) GetConsoleMode.invokeExact(stdOutHandle, currentMode);
			if (result == 0) {
			    throw new IllegalStateException("GetConsoleMode failed");
			}

			return currentMode.get(JAVA_INT, 0);
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
        	return 0;
		}
	}

	//-------------------------------------------------------------------
	private List<InputFlag> getInputFlags() {
		int mode = getInputMode();
		List<InputFlag> flags = new ArrayList<InputFlag>();
		List<InputFlag> notSet = new ArrayList<InputFlag>();
		for (InputFlag flag : InputFlag.values()) {
			if ( (mode & flag.value())>0) flags.add(flag); else notSet.add(flag);
		}
		logger.log(Level.DEBUG,"STD IN "+mode+"= "+String.join(",", flags.stream().map(f->f.name()).toList()));
		logger.log(Level.DEBUG,"STD IN not set = "+String.join(",", notSet.stream().map(f->f.name()).toList()));
		return flags;
	}

	//-------------------------------------------------------------------
	private void setInputMode(int current) {
		logger.log(Level.INFO ,"setInputMode({0})",current);
		try {
			int result = (int) SetConsoleMode.invokeExact(stdInHandle, current);
			if (result == 0) {
			    throw new IllegalStateException("SetConsoleMode failed");
			}
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
	}

	//-------------------------------------------------------------------
	private void setOutputMode(int current) {
		logger.log(Level.INFO ,"setOutputMode({0})",current);
		try {
			int result = (int) SetConsoleMode.invokeExact(stdOutHandle, current);
			if (result == 0) {
			    throw new IllegalStateException("SetConsoleMode failed");
			}
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
	}

	//-------------------------------------------------------------------
	private void setInputFlags(InputFlag...flags) {
		int current = getInputMode();
		for (InputFlag flag : flags)
			current |= flag.val;

		setInputMode(current);
	}

	//-------------------------------------------------------------------
	private void clearInputFlags(InputFlag...flags) {
		int current = getInputMode();
		logger.log(Level.INFO ,"clearInputFlags vorher: "+current);

		for (InputFlag flag : flags)
			current &= ~flag.val;

		setInputMode(current);
	}

	//-------------------------------------------------------------------
	private void setOutputFlags(OutputFlag...flags) {
		int current = getInputMode();

		for (OutputFlag flag : flags)
			current |= flag.val;

		setOutputMode(current);
	}

	//-------------------------------------------------------------------
	private void clearOutputFlags(OutputFlag...flags) {
		int current = getInputMode();
		logger.log(Level.DEBUG,"clearOutputFlags vorher: "+current);

		for (OutputFlag flag : flags)
			current &= ~flag.val;

		logger.log(Level.DEBUG,"clearOutputFlags nachher: "+current);
		setOutputMode(current);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getMode()
	 */
	@Override
	public TerminalMode getMode() {
		  int mode = getInputMode();
		  return ((mode & InputFlag.ENABLE_LINE_INPUT.value())>0)?TerminalMode.LINE_MODE:TerminalMode.RAW;
	}

	//-------------------------------------------------------------------
	public void debugModes() {
		  int mode = getInputMode();
		  List<String> flags = new ArrayList<String>();
		  List<String> notSet = new ArrayList<String>();
		  for (InputFlag flag : InputFlag.values()) {
			  if ( (mode & flag.value())>0) flags.add(flag.name()); else notSet.add(flag.name());
		  }
		  logger.log(Level.DEBUG,"STD IN "+mode+"= "+String.join(",", flags));
		  logger.log(Level.DEBUG,"STD IN not set = "+String.join(",", notSet));

		  mode = getOutputMode();
		  flags = new ArrayList<String>();
		  notSet.clear();
		  for (OutputFlag flag : OutputFlag.values()) {
			  if ( (mode & flag.value())>0) flags.add(flag.name()); else notSet.add(flag.name());
		  }
		  logger.log(Level.DEBUG,"STD OUT "+mode+"= "+String.join(",", flags));
		  logger.log(Level.DEBUG,"STD OUT not set = "+String.join(",", notSet));
	}

	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setMode(org.prelle.terminal.TerminalMode)
	 */
	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		int current = getInputMode();
		logger.log(Level.INFO,"IN SetConsoleMode vorher: "+current);

		current |= InputFlag.ENABLE_VIRTUAL_TERMINAL_INPUT.value();
		if (mode==TerminalMode.RAW) {
			current &= ~InputFlag.ENABLE_LINE_INPUT.value();
//			current &= ~InputFlag.ENABLE_PROCESSED_INPUT.value();
//			current &= ~InputFlag.ENABLE_ECHO_INPUT.value();
		} else {
			current |= InputFlag.ENABLE_LINE_INPUT.value();
//			current |= InputFlag.ENABLE_PROCESSED_INPUT.value();
//			current |= InputFlag.ENABLE_ECHO_INPUT.value();
		}
		logger.log(Level.INFO,"IN SetConsoleMode nachher: "+current);
		setInputMode(current);
		return this;
	}

//	//-------------------------------------------------------------------
//	public TerminalEmulator setOutputMode() {
//		IntByReference consoleMode = new IntByReference();
//		Kernel32.INSTANCE.GetConsoleMode(stdOutHandle, consoleMode);
//		int current = consoleMode.getValue();
//		System.out.println("OUT SetConsoleMode vorher: "+current);
//
//		//if (mode==TerminalMode.RAW) {
//			current |= OutputFlag.ENABLE_PROCESSED_OUTPUT.value();
////			current &= ~PROCESSED_INPUT;
//			current |= OutputFlag.ENABLE_VIRTUAL_TERMINAL_PROCESSING.value();
////		} else {
////			current |= ENABLE_LINE_INPUT;
//////			current |= PROCESSED_INPUT;
////			current |= ENABLE_ECHO_INPUT;
////		}
//		System.out.println("OUT SetConsoleMode nachher: "+current);
//		Kernel32.INSTANCE.SetConsoleMode(stdOutHandle, current);
//		return this;
//	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#isLocalEchoActive()
	 */
	@Override
	public boolean isLocalEchoActive() {
		  int mode = getInputMode();
		  return (mode & InputFlag.ENABLE_ECHO_INPUT.value())>0;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setLocalEchoActive(boolean)
	 */
	@Override
	public TerminalEmulator setLocalEchoActive(boolean localEcho) {
		if (localEcho) {
			setInputFlags(InputFlag.ENABLE_ECHO_INPUT);
		} else {
			clearInputFlags(InputFlag.ENABLE_ECHO_INPUT);
		}
		return this;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getOutputStream()
	 */
	@Override
	public ANSIOutputStream getOutputStream() {
		return out;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getInputStream()
	 */
	@Override
	public ANSIInputStream getInputStream() {
		return in;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getConsoleSize()
	 */
	@Override
	public int[] getConsoleSize() throws IOException, InterruptedException {
		try {
			int result = (int) GetConsoleScreenBufferInfo.invokeExact(stdOutHandle, consoleInfo);
			if (result == 0) {
			    throw new IllegalStateException("GetConsoleScreenBufferInfo failed");
			}
			// Größe des Konsolenfensters auslesen
			int sizeX = consoleInfo.get(JAVA_INT, consoleScreenBufferInfoLayout.byteOffset(MemoryLayout.PathElement.groupElement("dwSizeX")));
			int sizeY = consoleInfo.get(JAVA_INT, consoleScreenBufferInfoLayout.byteOffset(MemoryLayout.PathElement.groupElement("dwSizeY")));
			short windowLeft = consoleInfo.get(JAVA_SHORT, consoleScreenBufferInfoLayout.byteOffset(MemoryLayout.PathElement.groupElement("srWindowLeft")));
			short windowRight = consoleInfo.get(JAVA_SHORT, consoleScreenBufferInfoLayout.byteOffset(MemoryLayout.PathElement.groupElement("srWindowRight")));
			short windowTop = consoleInfo.get(JAVA_SHORT, consoleScreenBufferInfoLayout.byteOffset(MemoryLayout.PathElement.groupElement("srWindowTop")));
			short windowBottom = consoleInfo.get(JAVA_SHORT, consoleScreenBufferInfoLayout.byteOffset(MemoryLayout.PathElement.groupElement("srWindowBottom")));

			int width = windowRight - windowLeft + 1;
			int height = windowBottom - windowTop + 1;
			logger.log(Level.INFO, "Console size 1: {0}x{1}", sizeX, sizeY);
			logger.log(Level.INFO, "Console size 2: {0}x{1}", width, height);
			return new int[] {sizeX,sizeY};
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
        	return new int[] {-1,-1};
		}
	}
	
	//-------------------------------------------------------------------
	private int getInputCodepage() {
		try {
			int codePage = (int) getConsoleCP.invokeExact();
			if (codePage == 0) {
			    throw new IllegalStateException("getConsoleCP failed");
			}
			return codePage;
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
        	return -1;
		}
	}
	
	//-------------------------------------------------------------------
	private int getOutputCodepage() {
		try {
			int codePage = (int) getConsoleOutputCP.invokeExact();
			if (codePage == 0) {
			    throw new IllegalStateException("getConsoleOutputCP failed");
			}
			return codePage;
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
        	return -1;
		}
	}
	
	//-------------------------------------------------------------------
	private void setOutputCodepage(int codepage) {
		try {
			int result = (int) setConsoleOutputCP.invokeExact(codepage);
            if (result == 0) {
                throw new IllegalStateException("SetConsoleOutputCP failed");
            }
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
	}
	
	//-------------------------------------------------------------------
	private void setInputCodepage(int codepage) {
		try {
			int result = (int) setConsoleCP.invokeExact(codepage);
            if (result == 0) {
                throw new IllegalStateException("setConsoleCP failed");
            }
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
	}
	
	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getEncodings()
	 */
	@Override
	public Charset[] getEncodings() {
        int inputCP = getInputCodepage();
        int outputCP = getOutputCodepage();

        logger.log(Level.INFO, "Input codepage {0}", inputCP);
        logger.log(Level.INFO, "Output codepage {0}", outputCP);

        // Optional: Codepage als Java-Charset-Namen umwandeln
        Charset inputEncoding = codePageToEncoding(inputCP);
        Charset outputEncoding = codePageToEncoding(outputCP);

        logger.log(Level.INFO, "Input encoding {0}", inputEncoding);
        logger.log(Level.INFO, "Output encoding {0}", outputEncoding);
        return new Charset[] {inputEncoding, outputEncoding};
	}

	// Hilfsmethode zur Umwandlung einer Windows-Codepage in ein Java-Encoding
    public static Charset codePageToEncoding(int codePage) {
        switch (codePage) {
            case 65001: return StandardCharsets.UTF_8;
            case 1252: return Charset.forName("Windows-1252");
            case 850: return Charset.forName("IBM850");
            case 437: return Charset.forName("IBM437");
            // Fügen Sie hier weitere Codepages hinzu, falls erforderlich
            default: return Charset.forName("CP" + codePage); // Standardmäßig die Codepage-Nummer verwenden
        }
    }
}

