package org.prelle.realmrunner.gmcp;

import org.prelle.mud4j.gmcp.GMCPCommand;
import org.prelle.mudevents.PipeEvent;

/**
 * 
 */
record EventMapping(Class<? extends PipeEvent> mud, Class<? extends GMCPCommand> gmcp) {
	
	//-------------------------------------------------------------------
	public static EventMapping of(Class<? extends PipeEvent> mud, Class<? extends GMCPCommand> gmcp) {
		return new EventMapping(mud, gmcp);
	}

}
