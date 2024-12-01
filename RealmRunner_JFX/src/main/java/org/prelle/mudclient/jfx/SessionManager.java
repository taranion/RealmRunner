package org.prelle.mudclient.jfx;

/**
 * 
 */
public class SessionManager {
	
	public static Session createSession(String server, int port) {
		Session sess = new Session(server, port);
		return sess;
	}

}
