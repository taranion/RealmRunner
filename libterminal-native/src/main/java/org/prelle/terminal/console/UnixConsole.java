package org.prelle.terminal.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

/**
 *
 */
public class UnixConsole implements TerminalEmulator {

	/** Canonical input (erase and kill processing).  */
	private final static int ICANON = 0x00000002;
	/** Enable echo. */
	private final static int ECHO   = 0x00000008;

	// Definiere die Funktionen der C-Bibliothek
    public interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("c", CLibrary.class);

        int tcgetattr(int fd, Termios termios);
        int tcsetattr(int fd, int optionalActions, Termios termios);
        int getchar();
    }

    // Definition der termios-Struktur
    public static class Termios extends Structure {
    	public int c_iflag;
        public int c_oflag;
        public int c_cflag;
        public int c_lflag;
        public byte[] c_cc = new byte[32];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc");
        }
    }

    private Termios savedState;
    private Termios currentState;
    private ANSIOutputStream out;
    private ANSIInputStream in;

	//-------------------------------------------------------------------
	/**
	 */
	public UnixConsole() {
		// Fetch the current state
        savedState = new Termios();
        CLibrary.INSTANCE.tcgetattr(0, savedState);

        currentState = new Termios();
        currentState.c_iflag = savedState.c_iflag;
        currentState.c_oflag = savedState.c_oflag;
        currentState.c_cflag = savedState.c_cflag;
        currentState.c_lflag = savedState.c_lflag;
		System.arraycopy(savedState.c_cc, 0, currentState.c_cc, 0, savedState.c_cc.length);

        out = new ANSIOutputStream(System.out);
        in  = new ANSIInputStream(System.in);

        Thread restoreHook = new Thread( () -> {
        	CLibrary.INSTANCE.tcsetattr(0, 0, savedState);
        });
        Runtime.getRuntime().addShutdownHook(restoreHook);
	}

	//-------------------------------------------------------------------
	private boolean getFlag(int flag) {
		return  (currentState.c_lflag & flag)!=0;
	}

	//-------------------------------------------------------------------
	private void setFlag(int flag) {
//		System.out.println(String.format("set & %04x", flag));
//		int oldLFlag = currentState.c_lflag;
//		System.out.println(String.format(" in & %04x", oldCFlag));
		currentState.c_lflag = currentState.c_lflag | flag;
//		System.out.println(String.format("==> & %04x", currentState.c_lflag));
        CLibrary.INSTANCE.tcsetattr(0, 0, currentState);
//        System.out.println("Modified flags from "+oldLFlag+" to "+currentState.c_lflag);
	}

	//-------------------------------------------------------------------
	private void resetFlag(int flag) {
//		System.out.println(String.format("Reset & %04x", flag));
//		int oldLFlag = currentState.c_lflag;
//		System.out.println(String.format("   in & %04x", oldCFlag));
		currentState.c_lflag = currentState.c_lflag & ~flag;
//		System.out.println(String.format(" ===> & %04x", currentState.c_lflag));
        CLibrary.INSTANCE.tcsetattr(0, 0, currentState);
//        System.out.println(String.format("Change from %04x to %04x", oldLFlag, currentState.c_lflag));
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#getMode()
	 */
	@Override
	public TerminalMode getMode() {
		return getFlag(ICANON)?TerminalMode.LINE_MODE:TerminalMode.RAW;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setMode(org.prelle.terminal.TerminalMode)
	 */
	@Override
	public TerminalEmulator setMode(TerminalMode mode) {
		if (mode==TerminalMode.RAW) {
			// Deactivate canonical (line-editing) mode
			// Activate non-canonical (raw, direct) mode
			resetFlag(ICANON);
		} else {
			// Activate canonical (line-editing) mode
			setFlag(ICANON);
		}
		return this;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#isLocalEchoActive()
	 */
	@Override
	public boolean isLocalEchoActive() {
		return getFlag(ECHO);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.TerminalEmulator#setLocalEchoActive(boolean)
	 */
	@Override
	public TerminalEmulator setLocalEchoActive(boolean localEcho) {
		if (localEcho) {
			setFlag(ECHO);
		} else {
			resetFlag(ECHO);
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
	public int[] getConsoleSize() throws IOException, InterruptedException {
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
	public int[] getConsoleSize2() throws IOException, InterruptedException {
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

