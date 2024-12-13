package org.prelle.terminal.console;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

/**
 *
 */
public class UnixConsoleFFM implements TerminalEmulator {
	
	private final static Logger logger = System.getLogger("terminal.unix");

    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup libc = linker.defaultLookup();
	
	static enum LocalFlag {
		ISIG(0x0000001),
		/* Enable canonical mode */
		ICANON(0x0000002),
		XCASE(0x0000004),
		/* Echo input characters. */
		ECHO (0x0000008),
		ECHOE(0x0000020),
		ECHOK(0x0000040),
		;
		
		private int value;
		LocalFlag(int value) { this.value=value;}
	}

    // Offsets für die termios-Struktur (variiert je nach Plattform)
    private static final int TERM_STRUCT_SIZE = 48; // Beispielwert, überprüfen Sie Ihre Plattform
    private static final int LFLAG_OFFSET = 12;
    
    private static final int STDIN_FILENO = 0;
	
	private int savedState;
	private MemorySegment termios, winsize;
    private ANSIOutputStream out;
    private ANSIInputStream in;
    
    private Arena arena;
    private MethodHandle tcgetattr, tcsetattr, ioctl;
    
    private GroupLayout winsizeLayout = MemoryLayout.structLayout(
            JAVA_SHORT.withName("ws_row"),    // Zeilen
            JAVA_SHORT.withName("ws_col"),    // Spalten
            JAVA_SHORT.withName("ws_xpixel"), // Breite in Pixel
            JAVA_SHORT.withName("ws_ypixel")  // Höhe in Pixel
        );
    
	//-------------------------------------------------------------------
    public static void main(String[] args) {
    	new UnixConsoleFFM();
    }
    

	//-------------------------------------------------------------------
	/**
	 */
	public UnixConsoleFFM() {
        arena = Arena.ofShared();
        
        // Speicher für termios-Struktur
      	termios = arena.allocate(TERM_STRUCT_SIZE); // termios-Strukturgröße (Plattformabhängig)
      	winsize = arena.allocate(winsizeLayout);

            // Define functions
        	tcgetattr = linker.downcallHandle(
                libc.find("tcgetattr").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS)
            );
            tcsetattr = linker.downcallHandle(
                libc.find("tcsetattr").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS)
            );
            ioctl = Linker.nativeLinker().downcallHandle(
                    libc.find("ioctl").orElseThrow(() -> new IllegalStateException("ioctl not found")),
                    FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS)
                );

            // Terminaleinstellungen abrufen
            try {
            	int result = (int) tcgetattr.invoke(STDIN_FILENO, termios);
            	if (result != 0) {
            		throw new IllegalStateException("tcgetattr failed - are you eventually running in an IDE?");
            	}
            } catch (Throwable e) {
            	e.printStackTrace();
            }
 		savedState = getLFlag();
 		logger.log(Level.INFO, "Saved state is {0}", savedState);

        out = new ANSIOutputStream(System.out);
        in  = new ANSIInputStream(System.in);

        Thread restoreHook = new Thread( () -> {
        	logger.log(Level.INFO, "Resetting cooked mode and local echo");
        	setMode(TerminalMode.LINE_MODE);
        	setLocalEchoActive(true);
        });
        Runtime.getRuntime().addShutdownHook(restoreHook);
	}

	//-------------------------------------------------------------------
	private int getLFlag() {
        try {
        	int result = (int) tcgetattr.invoke(STDIN_FILENO, termios);
        	if (result != 0) {
        		throw new IllegalStateException("tcgetattr failed - are you eventually running in an IDE?");
        	}
        } catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
        }
 		logger.log(Level.INFO, "Current L-Flag is {0}", String.format("%08x",termios.get(JAVA_INT, LFLAG_OFFSET)));
        return termios.get(JAVA_INT, LFLAG_OFFSET);
	}

	//-------------------------------------------------------------------
	private boolean hasFlag(LocalFlag flag) {
		return (getLFlag() & flag.value)!=0;
	}

	//-------------------------------------------------------------------
	private void setFlag(LocalFlag flag) {
        int lflag = getLFlag() | flag.value;
 		logger.log(Level.INFO, "Set {0} in L-Flag to {1}", flag, String.format("%08x", lflag));
        termios.set(JAVA_INT, LFLAG_OFFSET, lflag);

        // Write termios struct
        try {
			int result = (int) tcsetattr.invoke(STDIN_FILENO, 0, termios);
			if (result != 0) {
			    throw new IllegalStateException("tcsetattr failed");
			}
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
	}

	//-------------------------------------------------------------------
	private void resetFlag(LocalFlag flag) {
        long lflag = getLFlag() & ~flag.value;
		logger.log(Level.INFO, "clear {0} in L-Flag to {1}", flag, lflag);
        termios.set(JAVA_INT, LFLAG_OFFSET, (int)lflag);

        try {
			int result = (int) tcsetattr.invoke(STDIN_FILENO, 0, termios);
			if (result != 0) {
			    throw new IllegalStateException("tcsetattr failed");
			}
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getMode()
	 */
	@Override
	public TerminalMode getMode() {
		return hasFlag(LocalFlag.ICANON)?TerminalMode.LINE_MODE:TerminalMode.RAW;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setMode(org.prelle.terminal.TerminalMode)
	 */
	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		logger.log(Level.INFO, "ENTER: setMode({0})", mode);
		if (mode==TerminalMode.RAW) {
			// Deactivate canonical (line-editing) mode
			// Activate non-canonical (raw, direct) mode
			resetFlag(LocalFlag.ICANON);
		} else {
			// Activate canonical (line-editing) mode
			setFlag(LocalFlag.ICANON);
		}
		logger.log(Level.INFO, "LEAVE: setMode({0})", mode);
		return this;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#isLocalEchoActive()
	 */
	@Override
	public boolean isLocalEchoActive() {
		return hasFlag(LocalFlag.ECHO);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setLocalEchoActive(boolean)
	 */
	@Override
	public TerminalEmulator setLocalEchoActive(boolean localEcho) {
		if (localEcho) {
			setFlag(LocalFlag.ECHO);
		} else {
			resetFlag(LocalFlag.ECHO);
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
	 * @see org.prelle.terminal.TerminalEmulator#getConsoleSize()
	 */
	@Override
	public int[] getConsoleSize() throws IOException, InterruptedException {
		int TIOCGWINSZ = 0x5413;

        try {
			// `ioctl`-Aufruf
			int result = (int) ioctl.invokeExact(0, TIOCGWINSZ, winsize);
			if (result != 0) {
			    throw new IllegalStateException("ioctl failed");
			}

			// Werte aus der winsize-Struktur auslesen
			short rows = winsize.get(JAVA_SHORT, winsizeLayout.byteOffset(MemoryLayout.PathElement.groupElement("ws_row")));
			short cols = winsize.get(JAVA_SHORT, winsizeLayout.byteOffset(MemoryLayout.PathElement.groupElement("ws_col")));
//			short xpixel = winsize.get(JAVA_SHORT, winsizeLayout.byteOffset(MemoryLayout.PathElement.groupElement("ws_xpixel")));
//			short ypixel = winsize.get(JAVA_SHORT, winsizeLayout.byteOffset(MemoryLayout.PathElement.groupElement("ws_ypixel")));

			return new int[] {cols,rows};
		} catch (Throwable e) {
        	logger.log(Level.ERROR, "Error invoking native function",e);
		}
        return new int[]{-1, -1};
    }

	//-------------------------------------------------------------------
	public int[] getConsoleSize2() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("sh","-c", "stty size < /dev/tty");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = br.readLine();
        process.waitFor();

        if (output != null) {
            String[] parts = output.split(" ");
            int rows = Integer.parseInt(parts[0]);
            int columns = Integer.parseInt(parts[1]);
            return new int[]{columns, rows};
        } else {
            return new int[]{-1, -1};
        }
    }

	//-------------------------------------------------------------------
	public int[] getConsoleSize3() throws IOException, InterruptedException {
		ProcessBuilder pbRows = new ProcessBuilder("tput", "lines");
        ProcessBuilder pbCols = new ProcessBuilder("tput", "cols");

        Process processRows = pbRows.start();
        Process processCols = pbCols.start();

        BufferedReader readerRows = new BufferedReader(new InputStreamReader(processRows.getInputStream()));
        BufferedReader readerCols = new BufferedReader(new InputStreamReader(processCols.getInputStream()));

        int rows = Integer.parseInt(readerRows.readLine().trim());
        int columns = Integer.parseInt(readerCols.readLine().trim());

        processRows.waitFor();
        processCols.waitFor();

        return new int[]{columns, rows};
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
	 * @see org.prelle.terminal.TerminalEmulator#getEncodings()
	 */
	@Override
	public Charset[] getEncodings() {
		if (System.getenv("LC_ALL")!=null && System.getenv("LC_ALL").contains("UTF-8")) {
			return new Charset[] {StandardCharsets.UTF_8, StandardCharsets.UTF_8};
		}
		return new Charset[] {StandardCharsets.ISO_8859_1, StandardCharsets.ISO_8859_1};
	}

}

