package de.log4j.mudclient;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;

import org.prelle.mud4j.gmcp.GMCPManager;
import org.prelle.mud4j.gmcp.Client.ClientPackage;
import org.prelle.mud4j.gmcp.Core.CorePackage;
import org.prelle.realmrunner.network.MUDSession;

/**
 * 
 */
public class TelnetTest {

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws Exception {
		GMCPManager.registerPackage(new CorePackage());
		GMCPManager.registerPackage(new ClientPackage());
		MUDSession session = MUDSession.builder()
				.withTarget(URI.create("telnet://tdome.nukefire.org:4000"))
				.withTarget(URI.create("telnet://game.petriamud.com:6600"))
				.withUserInterface(new TestUserInterface())
				.build();
		session.start();
		
		System.out.println("Connection started.");
		
		Thread.sleep(100000);
	}

}
