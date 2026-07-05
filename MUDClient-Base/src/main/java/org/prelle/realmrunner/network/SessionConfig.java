package org.prelle.realmrunner.network;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Builder
@Getter
@Setter
public class SessionConfig {

	private SessionProtocol protocol = SessionProtocol.TELNET;
	private String server;
	private int    port;
	private String login;
	private String passwd;

}
