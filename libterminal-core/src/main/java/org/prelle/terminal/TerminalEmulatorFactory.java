package org.prelle.terminal;

import java.util.ServiceLoader;

/**
 * 
 */
public class TerminalEmulatorFactory {

	public static TerminalEmulator createTerminal() {
		ServiceLoader<TerminalEmulator> loader = ServiceLoader.load(TerminalEmulator.class);
		for (TerminalEmulator terminal : loader) {
			return terminal;
		}
		throw new RuntimeException("No TerminalEmulator implementation found");
	}
	
}
