package org.prelle.realmrunner.gmcp;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

import org.prelle.mud4j.gmcp.GMCPHandler;
import org.prelle.mudevents.MUDEventProcessor;
import org.prelle.mudevents.PipeEvent;
import org.prelle.telnet.option.CommunicationRole;

/**
 * 
 */
public class GMCPLayer extends GMCPHandler{
	
	private final static Logger logger = System.getLogger("mud.client.telnet");
	
	private MUDEventProcessor receiveProcessor;
	private MUDEventProcessor sendProcessor;

	//-------------------------------------------------------------------
	public GMCPLayer(CommunicationRole role, String name, String version) {
		super(role, name, version);
		prepareReceiver();
		prepareSender();
	}

	//-------------------------------------------------------------------
	/**
	 * How to handle incoming data from the MUD. This is called by the MUDConnection when new data arrives.
	 */
	private void prepareReceiver() {
		receiveProcessor = new MUDEventProcessor() {
			@Override
			public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
				logger.log(Level.INFO, "RCV: {0} - {1}", event, event.getClass());
//					if (event instanceof Telnet binary) {
//					for (byte b : binary.getData()) {
//						ansi.parse(b & 0xff);
//					}
//					
//					return parserListener.consumeFragments().stream()
//							.map(frag -> (PipeEvent)new ANSIEvent(GMCPLayer.this,frag))
//							.toList();
//				} 
				return List.of(event);
			}
			public String getName() {
				return "ANSI";
			}
		};
	}

	//-------------------------------------------------------------------
	private void prepareSender() {
		sendProcessor = new MUDEventProcessor() {
			@Override
			public List<PipeEvent> onReceiveFromRemote(PipeEvent event) {
				logger.log(Level.INFO, "SND: {0}", event);
				
				
				return null;
			}
			public String getName() {
				return "GMCP";
			}
		};
	}

	//-------------------------------------------------------------------
	public MUDEventProcessor receiver() {
		return receiveProcessor;
	}

	//-------------------------------------------------------------------
	public MUDEventProcessor sender() {
		return sendProcessor;
	}

}
