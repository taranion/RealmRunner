package org.prelle.mudclient.network;

/**
 * 
 */
public class SessionManager {
	
	public static Session createSession(String server, int port) {
		Session sess = new Session(server, port);
		return sess;
	}

}
