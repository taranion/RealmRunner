package org.prelle.mudclient.network;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Setter
@Getter
public class Config extends AbstractConfig {

	private String server;
	private int    port;
	private String login;
	private String password;

	//-------------------------------------------------------------------
	/**
	 */
	public Config() {
		// TODO Auto-generated constructor stub
	}

}
