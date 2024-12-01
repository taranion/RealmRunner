package org.prelle.realmrunner.network;

import org.prelle.mud4j.gmcp.Char.Stats;
import org.prelle.mud4j.gmcp.Char.Vitals;
import org.prelle.mud4j.gmcp.Client.ClientMediaPlay;
import org.prelle.mud4j.gmcp.Room.GMCPRoomInfo;
import org.prelle.mud4j.gmcp.beip.BeipTilemapData;
import org.prelle.mud4j.gmcp.beip.BeipTilemapInfo;

/**
 *
 */
public interface MUDSessionGMCPListener {

	public void gmcpReceivedClientMedia(ClientMediaPlay play);

	public void gmcpReceivedRoomInfo(GMCPRoomInfo info);

	public void gmcpReceivedVitals(Vitals value);

	public void gmcpReceivedStats(Stats value);

	public void gmcpBeipTilemapInfo(BeipTilemapInfo info);

	public void gmcpBeipTilemapUpdate(BeipTilemapData data);

}
