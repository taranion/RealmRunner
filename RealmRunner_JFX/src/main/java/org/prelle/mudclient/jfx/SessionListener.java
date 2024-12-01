package org.prelle.mudclient.jfx;

import org.prelle.telnet.mud.MUDTilemapProtocol.TileMapData;

/**
 * 
 */
public interface SessionListener {
	
	public void textReceived(String mess);
	
	public void mapReceived(TileMapData data);

	public void connectionLost(Session session);

}
