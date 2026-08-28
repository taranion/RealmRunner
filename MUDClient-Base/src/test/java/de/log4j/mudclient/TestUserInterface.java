package de.log4j.mudclient;

import java.util.List;

import org.prelle.mudevents.PipeEvent;
import org.prelle.mudevents.ansi.ANSIEvent;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.MUDSessionUserInterface;
import org.prelle.terminal.TerminalEmulator;

/**
 * 
 */
public class TestUserInterface implements MUDSessionUserInterface {
	
	private TerminalEmulator terminal;

	//-------------------------------------------------------------------
	/**
	 */
	public TestUserInterface() {
		terminal = new TestTerminal();
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.PipeEvent)
	 */
	@Override
	public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
		return List.of(event);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDSessionUserInterface#getTerminal()
	 */
	@Override
	public TerminalEmulator getTerminal() {
		// TODO Auto-generated method stub
		return terminal;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDSessionUserInterface#indicateFeatureState(java.lang.String, boolean)
	 */
	@Override
	public void indicateFeatureState(String feature, boolean state) {
		// TODO Auto-generated method stub

	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.MUDSessionUserInterface#connectWithSession(org.prelle.realmrunner.network.MUDSession)
	 */
	@Override
	public void connectWithSession(MUDSession session) {
		// TODO Auto-generated method stub

	}

}
