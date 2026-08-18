package org.prelle.realmrunner.network;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

import org.prelle.mudansi.FormatUtil;
import org.prelle.mudansi.OutputFormatter.ANSIOutputConfig;
import org.prelle.mudansi.TextWithMarkup;
import org.prelle.muddown.MessageEnvelope;
import org.prelle.muddown.MuddownMessage;
import org.prelle.muddown.MuddownParser;
import org.prelle.muddown.RoomMessage;
import org.prelle.mudevents.MUDEvent;
import org.prelle.mudevents.MUDEventProcessor;

/**
 * 
 */
public class ClientMUDDownParser implements MUDEventProcessor, MUDEvent {
	
	private final static Logger logger = System.getLogger("muddown");

	//-------------------------------------------------------------------
	/**
	 */
	public ClientMUDDownParser() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.mudevents.MUDEventProcessor#apply(org.prelle.mudevents.MUDEvent)
	 */
	@Override
	public List<MUDEvent> apply(MUDEvent event) {
		if (event instanceof MessageEnvelope muddown) {
			return processMudDown(muddown);			
		}
		return List.of(event);
	}

	private List<MUDEvent> processMudDown(MessageEnvelope muddown) {
		logger.log(Level.INFO, "TODO: "+muddown);
		switch (muddown.getType()) {
		case ROOM:
			return parseRoomBlock(muddown.getMuddown());
		default:
			logger.log(Level.WARNING, "Unhandled MUDDown message type: "+muddown.getType());
		}
		return List.of();
	}

	private List<MUDEvent> parseRoomBlock(String markdown) {
		RoomMessage room = MuddownParser.parse(markdown, RoomMessage.class);
		logger.log(Level.INFO, "room = "+room);
		
		ANSIOutputConfig config = ANSIOutputConfig.builder()
				.width(72)
				.useOSCLinks(true)
				.build();
		
		TextWithMarkup markup = FormatUtil.parseMUDDown(markdown);
		String ansi = FormatUtil.renderToANSI(markup, config);
		System.out.println("ANSI:\n"+ansi);
		return null;
	}

}
