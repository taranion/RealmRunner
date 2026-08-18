package de.log4j.mudclient;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;

import org.prelle.mud4j.gmcp.GMCPManager;
import org.prelle.mud4j.gmcp.Client.ClientPackage;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.WebsocketMUDConnection;

/**
 * 
 */
public class WSSTest {

	//-------------------------------------------------------------------
	/**
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws Exception {
		GMCPManager.registerPackage(new ClientPackage());
		
		// wss://arkadia.rpg.pl/wss
		// ws://muddown.com
		MUDSession session = MUDSession.builder()
				.withTarget(URI.create("wss://muddown.com/ws"))
				.withUserInterface(new TestUserInterface())
				.withWebsocketSubprotocol(WebsocketMUDConnection.SUBPROTOCOL_MUDDOWN)
				.build();
		session.start();
		
		System.out.println("Connection started. Press Enter to close.");
		
		Thread.sleep(100000);
	}

}
