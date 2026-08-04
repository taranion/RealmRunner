package org.prelle.realmrunner.network;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import org.prelle.realmrunner.network.SoundManager.PlayCommand;
import org.prelle.realmrunner.network.SoundManager.SoundType;
import org.prelle.terminal.ReceiveBuffer;
import org.prelle.terminal.ReceiveBuffer.HandlerResult;
import org.prelle.terminal.ReceiveBuffer.ReadBufferHandler;
import org.prelle.terminal.ReceiveBuffer.ReceivedLine;

/**
 * 
 */
public class MSPHandler implements ReadBufferHandler {

	private final static Logger logger = System.getLogger("mud.client.msp");
	
	private MUDSession session;
	private Path soundPath;
	private Path musicPath;

	//-------------------------------------------------------------------
	public MSPHandler(MUDSession session) {
		this.session = session;
		try {
			soundPath = DataFileManager.getCurrentDataDir(session).resolve("sounds");
			musicPath = DataFileManager.getCurrentDataDir(session).resolve("music");
		} catch (IOException e) {
			logger.log(Level.ERROR, "Failed accessing directories for sound/music: {0}", e);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.ReceiveBuffer.ReadBufferHandler#onLineReceived(java.lang.String, java.util.List)
	 */
	@Override
	public HandlerResult onLineReceived(ReceivedLine rcvLine, List<ReceivedLine> history) {
		String line = rcvLine.getOriginalAsText();
		logger.log(Level.ERROR, "Check "+line);
		if (line.startsWith("!!MUSIC") || line.startsWith("!!SOUND")) {
			logger.log(Level.INFO, "MSP command: {0}", line);
			System.err.println("MSP command: "+line);
			
			PlayCommand command = parse(
					line.startsWith("!!MUSIC")?SoundType.MUSIC:SoundType.SOUND,
					line.substring(
					line.indexOf('(')+1, 
					line.lastIndexOf(')')
					));
			// ToDo: COnsult a session directory for cached files
			
			if (command.filename.equalsIgnoreCase("off")) {
				SoundManager.getInstance().stop(session, command);
				return new HandlerResult(true, true, null); // line has been consumed
			}
			
			// Convert to URL
			String fullUrl = command.fullUrl.endsWith("/")?command.fullUrl:(command.fullUrl+"/");
			fullUrl += command.filename;
			
			try {
				
				command.path = DataFileManager.downloadFileTo(session,command.filename, URI.create(fullUrl));
				SoundManager.getInstance().play(session, command);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return new HandlerResult(true, true, null); // line has been consumed
		}
		return new HandlerResult(false, false, null); // line has been consumed
	}

	//-------------------------------------------------------------------
	/**
	 * Parse a line like this:
	 * <code>
	 * audio/01_Ultima_Theme.mp3 V=75 L=-1 C=1 U=http://eden-test.rpgframework.de:4080
	 * audio/01_Ultima_Theme.mp3 V =  75 L= -1 C =1 U=http://eden-test.rpgframework.de:4080
	 * </code>
	 */
	public static PlayCommand parse(SoundType type, String line) {
		PlayCommand command = new PlayCommand();
		command.soundType = type;
		command.volume = 100;
		command.loops = 1;
		command.priority = 50;
		command.cont = true;

		if (line == null) {
			return command;
		}

		line = line.trim();
		if (line.isEmpty()) {
			return command;
		}

		String fname;
		String paramString;

		if (line.startsWith("'") || line.startsWith("\"")) {
			char quote = line.charAt(0);
			int endQuote = line.indexOf(quote, 1);
			if (endQuote != -1) {
				fname = line.substring(1, endQuote);
				paramString = line.substring(endQuote + 1).trim();
			} else {
				fname = line.substring(1).trim();
				paramString = "";
			}
		} else {
			int firstSpace = -1;
			for (int i = 0; i < line.length(); i++) {
				if (Character.isWhitespace(line.charAt(i))) {
					firstSpace = i;
					break;
				}
			}
			if (firstSpace != -1) {
				fname = line.substring(0, firstSpace);
				paramString = line.substring(firstSpace + 1).trim();
			} else {
				fname = line;
				paramString = "";
			}
		}

		if ((fname.startsWith("'") && fname.endsWith("'")) || (fname.startsWith("\"") && fname.endsWith("\""))) {
			if (fname.length() >= 2) {
				fname = fname.substring(1, fname.length() - 1);
			}
		}

		command.filename = fname;

		if (!paramString.isEmpty()) {
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)\\b([VLPCTU])\\s*=\\s*('[^']*'|\"[^\"]*\"|\\S+)");
			java.util.regex.Matcher matcher = pattern.matcher(paramString);

			while (matcher.find()) {
				String keyStr = matcher.group(1);
				String val = matcher.group(2);

				if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
					if (val.length() >= 2) {
						val = val.substring(1, val.length() - 1);
					}
				}

				char key = Character.toUpperCase(keyStr.charAt(0));
				switch (key) {
				case 'V':
					try {
						command.volume = Integer.parseInt(val);
					} catch (NumberFormatException e) {
						logger.log(Level.WARNING, "Invalid volume number: {0}", val);
					}
					break;
				case 'L':
					try {
						command.loops = Integer.parseInt(val);
					} catch (NumberFormatException e) {
						logger.log(Level.WARNING, "Invalid loops number: {0}", val);
					}
					break;
				case 'P':
					try {
						command.priority = Integer.parseInt(val);
					} catch (NumberFormatException e) {
						logger.log(Level.WARNING, "Invalid priority number: {0}", val);
					}
					break;
				case 'C':
					command.cont = "1".equals(val) || "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val);
					break;
				case 'T':
					command.type = val;
					break;
				case 'U':
					command.fullUrl = val;
					break;
				}
			}
		}

		return command;
	}
	

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.ReceiveBuffer.ReadBufferHandler#onConnectionLost()
	 */
	@Override
	public void onConnectionLost() {
		// TODO Auto-generated method stub

	}

}
