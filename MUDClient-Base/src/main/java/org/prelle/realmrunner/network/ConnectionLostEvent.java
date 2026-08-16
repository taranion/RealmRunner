package org.prelle.realmrunner.network;

import org.prelle.mudevents.AMUDEvent;
import org.prelle.mudevents.MUDEvent;

/**
 * 
 */
public class ConnectionLostEvent extends AMUDEvent implements MUDEvent {

	//-------------------------------------------------------------------
	/**
	 */
	public ConnectionLostEvent(Object src) {
		super(src);
	}

}
