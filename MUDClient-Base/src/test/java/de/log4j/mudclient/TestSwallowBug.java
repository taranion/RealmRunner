package de.log4j.mudclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.prelle.ansi.FilteringANSIStream;
import org.prelle.ansi.ANSIInputStream;
import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.terminal.EchoChamber;
import org.prelle.terminal.SwitchableInputStream;
import org.prelle.terminal.SwitchableOutputStream;
import org.prelle.terminal.TerminalEmulator;
import org.prelle.terminal.TerminalMode;

import lombok.Getter;

public class TestSwallowBug {

	public TestSwallowBug() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws  IOException {
		// TODO Auto-generated method stub
//		TCPMUDConnection con = new TCPMUDConnection(InetAddress.getLocalHost(), 4000);
//		InputStream in = con.getStreamFromMUD();
//		OutputStream out = con.getStreamToMUD();
		
		Config connectWith = new Config();
		connectWith.setServer("localhost");
		connectWith.setPort(4000);
		
		DummyTerminal terminal = new DummyTerminal();
		MUDSession session = MUDSession.builder(terminal)
				.setClientConfig(connectWith)
				.setTerminalTypes("Realm Runner","xterm","MTTS 267")
				.build();
		
		System.out.println("Session created: "+session);
		byte[] buf = new byte[2048];
		while (true) {
			int len = terminal.getInPipe().read(buf);
			if (len==-1) break;
			// Convert to string and print
			String text = new String(buf, 0, len, Charset.defaultCharset());
			System.out.print(text);
			if (text.contains("elnet negotiation done")) {
				if (text.startsWith("elnet negoti")) {
					System.err.println("Error - the line should have been started with 'Telnet'");
					break;
				}
				break;
			}
		}
		
	}

	/**
	 * This class is modeled after GhosttyTerminalView
	 */
	private static class DummyTerminal implements TerminalEmulator {
		private final static System.Logger logger = System.getLogger("ghostty");

		@Getter
		private SwitchableInputStream inPipe= new SwitchableInputStream();
		@Getter
		private SwitchableOutputStream outPipe= new SwitchableOutputStream();
		@Getter
	   private ANSIInputStream pin;
		    private boolean localEcho = true;
		    private EchoChamber echoChamber;

		    
		@Override
		public TerminalMode getMode() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TerminalEmulator setMode(TerminalMode mode) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isLocalEchoActive() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public TerminalEmulator setLocalEchoActive(boolean value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int[] getConsoleSize() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addConsoleSizeListener(Consumer<int[]> listener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Charset[] getEncodings() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void releaseInputBuffer() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendUserInput(String text) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public FilteringANSIStream connectWith(InputStream in, OutputStream out) {
			pin = new ANSIInputStream(in);
			//PassOutANSIOutputStream pout = new PassOutANSIOutputStream(out, (fragmentSent) -> handleFragmentSent(fragmentSent));
			echoChamber = new EchoChamber(out, inPipe);
			outPipe.setSink(echoChamber);
			inPipe.setSource(pin);
			
			
			try {
				logger.log(Level.WARNING, "Output: "+this.outPipe);
				logger.log(Level.WARNING, "Input : "+this.inPipe);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return pin;
		}

		@Override
		public void start() {
			// TODO Auto-generated method stub
		}
	
	}
}
