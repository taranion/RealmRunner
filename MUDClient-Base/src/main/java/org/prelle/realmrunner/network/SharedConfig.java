package org.prelle.realmrunner.network;

import lombok.Data;

/**
 * This is the general client-wide configuration, which is not specific to a world.
 */
@Data
public class SharedConfig {
	
	private Boolean useTTS;
	private Boolean useAutoTranslate;

	//-------------------------------------------------------------------
	/**
	 */
	public SharedConfig() {
		// TODO Auto-generated constructor stub
	}

}
