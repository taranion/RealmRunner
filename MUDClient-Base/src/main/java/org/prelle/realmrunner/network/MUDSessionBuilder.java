package org.prelle.realmrunner.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.prelle.telnet.CommunicationRole;
import org.prelle.telnet.TelnetCommand;
import org.prelle.telnet.TelnetInputStream;
import org.prelle.telnet.TelnetListener;
import org.prelle.telnet.TelnetOutputStream;
import org.prelle.telnet.TelnetProtocol;
import org.prelle.telnet.TelnetSubnegotiationHandler;
import org.prelle.terminal.TerminalEmulator;

public class MUDSessionBuilder {
	
	private final static Logger logger = MUDSession.logger;
	
	private static record TelnetResult(TelnetInputStream in, TelnetOutputStream out, TelnetProtocol protocol) {}

	TerminalEmulator terminal;
	private Config clientConfig;
	private SessionConfig sessionData;
	private Charset charset;
	String[] terminalTypes;
	TelnetProtocol telnet;
	
	//-------------------------------------------------------------------
	public MUDSessionBuilder(TerminalEmulator terminal) {
		this.terminal = terminal;
	}
	
	//-------------------------------------------------------------------
	public MUDSession build() throws IOException {
		try {
			InetAddress host = InetAddress.getByName(clientConfig.getServer());
			
			MUDConnection con = switch (clientConfig.getProtocol()) {
				case TELNET    -> new TCPMUDConnection(host, clientConfig.getPort());
				case WEBSOCKET -> new WebsocketMUDConnection(host, clientConfig.getPort());
				default -> throw new IllegalArgumentException("Unsupported protocol "+clientConfig.getProtocol());
			};
			MUDSession.logger.log(Level.DEBUG, "New connection: {0}", con);
			InputStream in = con.getStreamFromMUD();
			OutputStream out = con.getStreamToMUD();
			
			boolean isTelnet = con instanceof TCPMUDConnection || (con instanceof WebsocketMUDConnection ws && ws.getNegotiatedSubprotocol().contains("telnet"));
			
			if (isTelnet) {
				TelnetResult result = initializeTelnet(in,out);
				in = result.in;
				out = result.out;
				telnet = result.protocol;
			}
			
			MUDSession session = new MUDSession(terminal, in, out, clientConfig, this);
			if (in instanceof TelnetInputStream tin) {
				tin.getProtocol().addListener(session);
			}
			return session;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
		return null;
	}
	//-------------------------------------------------------------------
	public MUDSessionBuilder setCharset(Charset value) { this.charset = value; return this; }
	//-------------------------------------------------------------------
	public MUDSessionBuilder setConfig(SessionConfig value) { this.sessionData = value; return this; }
	//-------------------------------------------------------------------
	public MUDSessionBuilder setClientConfig(Config value) { this.clientConfig = value; return this; }
	//-------------------------------------------------------------------
	public MUDSessionBuilder setTerminalTypes(String...value) { this.terminalTypes = value; return this; }
	
	//-------------------------------------------------------------------
	private TelnetResult initializeTelnet(InputStream in, OutputStream out) {
		logger.log(Level.WARNING, "ENTER: initializeTelnet");			
		try {
			TelnetProtocol protocol = new TelnetProtocol(CommunicationRole.CLIENT);
//			MUDSession.configureTelnetProtocol(this, protocol, clientConfig);
			out = new TelnetOutputStream(out, protocol);
			protocol.setOutputStream((TelnetOutputStream) out);
			in = new TelnetInputStream(in, protocol);
			protocol.setInputStream( (TelnetInputStream) in);
			((TelnetInputStream)in).setReverseStream((TelnetOutputStream) out);
			return new TelnetResult((TelnetInputStream)in, (TelnetOutputStream)out, protocol);
		} catch (Throwable e) {
			logger.log(Level.ERROR, "Error initializing telnet protocol", e);
			return null;
		} finally {
			logger.log(Level.WARNING, "LEAVE: initializeTelnet");			
		}
	}
}