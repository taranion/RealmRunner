package org.prelle.realmrunner.network;

import org.prelle.mudevents.MUDEventProcessor;
import org.prelle.terminal.TerminalEmulator;

/**
 * 
 */
public interface MUDSessionUserInterface extends MUDEventProcessor{
	
	public TerminalEmulator getTerminal();
	
	public void indicateFeatureState(String feature, boolean state);

	//-------------------------------------------------------------------
	/**
	 * @param session
	 */
	public void connectWithSession(MUDSession session);
	
}
