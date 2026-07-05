package org.prelle.realmrunner.network;

import org.prelle.terminal.TerminalEmulator;

/**
 * 
 */
public interface MUDSessionUserInterface {
	
	public TerminalEmulator getTerminal();
	
	public void indicateFeatureState(String feature, boolean state);

}
