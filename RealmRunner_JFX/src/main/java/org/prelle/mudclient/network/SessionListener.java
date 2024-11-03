package org.prelle.mudclient.network;

import org.prelle.telnet.mud.MUDTilemapProtocol.TileMapData;

/**
 * 
 */
public interface SessionListener {
	
	public void textReceived(String mess);
	
	public void mapReceived(TileMapData data);

	public void connectionLost(Session session);

}
