package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.mud4j.gmcp.GMCPManager;
import org.prelle.mud4j.gmcp.Char.CharPackage;
import org.prelle.mud4j.gmcp.Char.Stats;
import org.prelle.mud4j.gmcp.Char.Vitals;
import org.prelle.mud4j.gmcp.CharSkills.CharSkillsPackage;
import org.prelle.mud4j.gmcp.Client.ClientMediaPackage;
import org.prelle.mud4j.gmcp.Client.ClientMediaPlay;
import org.prelle.mud4j.gmcp.Client.ClientMediaStop;
import org.prelle.mud4j.gmcp.Room.GMCPRoomInfo;
import org.prelle.mud4j.gmcp.beip.BeipTilemapData;
import org.prelle.mud4j.gmcp.beip.BeipTilemapInfo;
import org.prelle.telnet.TelnetConstants.ControlCode;
import org.prelle.telnet.TelnetInputStream;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetOptionListener;
import org.prelle.telnet.TelnetOptionRegistry;
import org.prelle.telnet.TelnetOutputStream;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSocket.State;
import org.prelle.telnet.TelnetSocketListener;
import org.prelle.telnet.mud.AardwolfMushclientProtocol;
import org.prelle.telnet.mud.GenericMUDCommunicationProtocol.GMCPReceiver;
import org.prelle.telnet.mud.GenericMUDCommunicationProtocol.RawGMCPMessage;
import org.prelle.telnet.mud.MUDTerminalTypeData;
import org.prelle.telnet.option.TelnetWindowSize;

import lombok.Getter;

/**
 *
 */
@Getter
public class MUDSession implements TelnetSocketListener, TelnetOptionListener, GMCPReceiver {

	private final static Logger logger = System.getLogger("mud.client");

	private TelnetSocket socket;
	private ANSIOutputStream streamToMUD;
	private TelnetInputStream streamFromMUD;
	private Thread thread;

	private boolean characterMode = false;

	private TelnetWindowSize optNAWS;
	private MUDSessionGMCPListener gmcpListener;

	//-------------------------------------------------------------------
	public MUDSession(SessionConfig session, TelnetSocketListener callback, int[] naws) throws IOException {
		logger.log(Level.INFO, "ENTER: MUDSession.<init>");
		TelnetOptionRegistry.register(TelnetOption.MUSHCLIENT.getCode(), new AardwolfMushclientProtocol());

		GMCPManager.registerPackage(new ClientMediaPackage());
		GMCPManager.registerPackage(new CharPackage());
		GMCPManager.registerPackage(new CharSkillsPackage());

		// Detect terminal type
		String term = System.getenv("TERM");
		if (term==null) term="xterm";
		// Detect environment data
		Map<String,String> environment = detectEnvironment();
		MUDTerminalTypeData mttData = new MUDTerminalTypeData()
				.setClientName("RealmRunner")
				.setTerminalType(term)
				;

		logger.log(Level.INFO, "Connecting to {0} port {1}", session.getServer(), session.getPort());
		socket = new TelnetSocket(session.getServer(), session.getPort())
				.addSocketListener(this)
				.addSocketListener(callback)
				.setOptionListener(TelnetOption.ECHO, this)
//				.addSocketListener(new GMCPHandler(true))
				.support(TelnetOption.ECHO.getCode(), ControlCode.WILL)
				.support(TelnetOption.SGA.getCode(), ControlCode.DO)
				.support(TelnetOption.EOR.getCode(), ControlCode.DO)
				.support(TelnetOption.NEW_ENVIRON.getCode(), ControlCode.WILL, environment)
				.support(TelnetOption.NAWS.getCode(), ControlCode.WILL, naws)
				.support(TelnetOption.LINEMODE.getCode(), ControlCode.WILL)
				.support(TelnetOption.TERMINAL_TYPE.getCode(), ControlCode.WILL, mttData)
				.support(TelnetOption.MSP.getCode(), ControlCode.DO)
				.support(TelnetOption.MXP.getCode(), ControlCode.DO)
				.support(TelnetOption.GMCP.getCode(), ControlCode.DO)
				.support(TelnetOption.MUSHCLIENT.getCode(), ControlCode.DO)
//				.support(new MUDClientCompression1(), Role.REJECT_OUTRIGHT)
//				.support(new MUDClientCompression2(), Role.REJECT_OUTRIGHT)
//				.support(new ZenithMUDProtocol(), Role.REJECT_OUTRIGHT)
				;
		socket.setTcpNoDelay(true);
		logger.log(Level.INFO, "Register MUDSession as GMCP listener");
		socket.setOptionListener(TelnetOption.GMCP, this);
		streamToMUD   = new ANSIOutputStream( socket.getOutputStream());
		streamFromMUD = (TelnetInputStream) socket.getInputStream();
		logger.log(Level.INFO, "LEAVE: MUDSession.<init>");
	}

	//-------------------------------------------------------------------
	/**
	 * @return
	 */
	private Map<String, String> detectEnvironment() {
		Map<String,String> ret = new HashMap<>();
		for (Entry<String, String> entry : System.getenv().entrySet()) {
			switch (entry.getKey()) {
			case "COLORTERM":
			case "HOME":
			case "HOSTNAME":
			case "HOSTTYPE":
			case "KITTY_PUBLIC_KEY":
			case "LANG":
			case "TERM":
			case "USER":
			case "USERNAME":
				ret.put(entry.getKey(), entry.getValue());
				break;
			}
		}
		return ret;
	}

	//-------------------------------------------------------------------
	public void sendWindowSizeUpdate(int width, int height) throws IOException {
		TelnetWindowSize.sendUpdate(socket, width, height);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetOptionStatusChange(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetOption, boolean)
	 */
	@Override
	public void telnetOptionStatusChange(TelnetSocket nvt, TelnetOption option, boolean active) {
		// TODO Auto-generated method stub

	}


	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.TelnetSocketListener#telnetSocketChanged(org.prelle.telnet.TelnetSocket, org.prelle.telnet.TelnetSocket.State, org.prelle.telnet.TelnetSocket.State)
	 */
	@Override
	public void telnetSocketChanged(TelnetSocket nvt, State oldState, State newState) {
		logger.log(Level.DEBUG, "state changed "+newState);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.mud.GenericMUDCommunicationProtocol.GMCPReceiver#telnetReceiveGMCP(org.prelle.telnet.mud.GenericMUDCommunicationProtocol.RawGMCPMessage)
	 */
	@Override
	public void telnetReceiveGMCP(RawGMCPMessage gmcp) {
		logger.log(Level.WARNING, "GMCP RCV "+gmcp.getNamespace()+"  "+gmcp.getMessage());
		Object mess = GMCPManager.decode(gmcp.getNamespace(), gmcp.getMessage());
		if (mess==null) {
			logger.log(Level.WARNING, "No parsing support for {0} {1}", gmcp.getNamespace(), gmcp.getMessage());
			return;
		}
		if (gmcpListener==null) {
			logger.log(Level.WARNING, "No handler for GMCP "+mess);
			return;
		}

		switch (mess) {
		case BeipTilemapInfo info -> gmcpListener.gmcpBeipTilemapInfo(info);
		case BeipTilemapData data -> gmcpListener.gmcpBeipTilemapUpdate(data);
		case ClientMediaPlay play -> gmcpListener.gmcpReceivedClientMedia(play);
		case ClientMediaStop stop -> gmcpListener.gmcpReceivedClientMedia(stop);
		case GMCPRoomInfo room -> gmcpListener.gmcpReceivedRoomInfo(room);
		case Stats stats -> gmcpListener.gmcpReceivedStats(stats);
		case Vitals vitals -> gmcpListener.gmcpReceivedVitals(vitals);
		case String strMess when gmcp.getNamespace().equals("Core.Goodbye") -> {
			logger.log(Level.WARNING, "Server closed connection with message ''{0}''", strMess);
			close();
		}
		default -> {
			logger.log(Level.WARNING, "Don't know what to do for "+mess);
		}
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @param gmcpListener the gmcpListener to set
	 */
	public void setGmcpListener(MUDSessionGMCPListener gmcpListener) {
		this.gmcpListener = gmcpListener;
	}

	//-------------------------------------------------------------------
	public void close() {
		logger.log(Level.WARNING, "closing session");
		try {
			streamToMUD.close();
			streamFromMUD.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
