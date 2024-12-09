package org.prelle.realmrunner.network;

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
	private String serverEncoding;

	//-------------------------------------------------------------------
	/**
	 */
	public Config() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	public Config(AbstractConfig copy) {
		super(copy);
	}

}
