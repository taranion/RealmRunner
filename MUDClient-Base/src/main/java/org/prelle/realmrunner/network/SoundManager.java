package org.prelle.realmrunner.network;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
public abstract class SoundManager {

	protected final static Logger logger = System.getLogger("mud.sound");

	public static enum SoundType {
		SOUND,
		MUSIC
	}


	public static class PlayCommand {
		public SoundType soundType;
		public String filename;
		public int loops;
		public int volume;
		public int priority;
		public boolean cont;
		public String type;
		public String fullUrl;
		public Path path;
	}

	@Getter @Setter
	public static class NowPlaying {
		private SoundType soundType;
		private String id;
		private Path path;
		private int remainingLoops;
		private int originalVolume;
		/** The item which the underlying player implementation uses to handle the object */
		private Object mediaPlayerObject;
	}
	
	private static SoundManager instance;
	
	private static Map<MUDSession, List<NowPlaying>> nowPlaying = new HashMap<>();

	//-------------------------------------------------------------------
	/**
	 */
	public SoundManager() {
		SoundManager.instance = this;
	}
	
	public static SoundManager getInstance() { 
		return instance;
	}

//	//-------------------------------------------------------------------
//	/**
//	 * @see org.prelle.telnet.mud.MUDSoundProtocolListener#mspReceivedCommand(java.lang.String)
//	 */
//	@Override
//	public void mspReceivedCommand(String mspCommand) {
//		logger.log(Level.WARNING, "TODO: "+mspCommand);
//		PlayCommand com = convertMSP(mspCommand);
//		logger.log(Level.WARNING, "com = "+com);
//		try {
//			if (com.url!=null) {
//				logger.log(Level.WARNING, "URI ="+com.url);
//				URI uri = URI.create(com.url);
//				Path result = DataFileManager.downloadFileTo(com.filename, uri);
//				if (result.getFileName().toString().endsWith(".mp3"))
//					playMP3(result, com.volume);
//				else
//					playWav(result, com.volume);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	//-------------------------------------------------------------------
	protected static NowPlaying isPlaying(MUDSession session, String id) {
		if (!nowPlaying.containsKey(session)) return null;
		return nowPlaying.get(session).stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
	}

	//-------------------------------------------------------------------
	public static PlayCommand convertMSP(String mspLine) {
		if (mspLine == null) return null;
		SoundType type;
		if (mspLine.startsWith("!!SOUND(")) {
			type = SoundType.SOUND;
		} else if (mspLine.startsWith("!!MUSIC(")) {
			type = SoundType.MUSIC;
		} else
			throw new IllegalArgumentException("Not a valid MSP line: "+mspLine);
		int openParen = mspLine.indexOf('(');
		int closeParen = mspLine.lastIndexOf(')');
		String data = (openParen != -1 && closeParen > openParen) ? mspLine.substring(openParen + 1, closeParen) : "";
		return MSPHandler.parse(type, data);
	}

	//-------------------------------------------------------------------
	private static void assignTo(PlayCommand com, Character currentType, StringBuffer collect) {
		logger.log(Level.DEBUG, "Parse type {0} = {1}", currentType, collect);
		if (currentType==null) {
			logger.log(Level.DEBUG, "Parse filename = {0} ", collect);
			com.filename=collect.toString();
			collect.delete(0, collect.length());
		} else {
			switch (Character.toLowerCase(currentType)) {
			case 'v': com.volume=Integer.parseInt(collect.toString()); break;
			case 'l': com.loops=Integer.parseInt(collect.toString()); break;
			case 'p': com.priority=Integer.parseInt(collect.toString()); break;
			case 'c': com.cont  = Boolean.parseBoolean( collect.toString()); break;
			case 't': com.type=collect.toString(); break;
			case 'u': com.fullUrl =collect.toString(); break;
			}
		}
	}

	//-------------------------------------------------------------------
	public abstract void stopInternal(MUDSession session, PlayCommand command, NowPlaying playing);

	//-------------------------------------------------------------------
	public void stop(MUDSession session, PlayCommand command) {
		if (command==null) return;
		if (command.path==null) {
			logger.log(Level.WARNING, "SoundManager.stop: No path for {0}", command.filename);
			return;
		}
		NowPlaying nowPlayingItem = isPlaying(session, command.filename);
		if (nowPlayingItem != null) {
			stopInternal(session, command, nowPlayingItem);
			nowPlaying.get(session).remove(nowPlayingItem);
		}
	}

	//-------------------------------------------------------------------
	protected abstract NowPlaying startInternal(MUDSession session, PlayCommand command);

	//-------------------------------------------------------------------
	public void play(MUDSession session, PlayCommand command) {
		if (command==null) return;
		if (command.path==null) {
			logger.log(Level.WARNING, "SoundManager.play: No path for {0}", command.filename);
			return;
		}
		NowPlaying nowPlayingItem = isPlaying(session, command.filename);
		if (nowPlayingItem != null) {
			// File is already playing
			if (command.cont) {
				// But continuation is requested, so we are done
				return;
			} else {
				// But since it should not be continued, we stop it and start it again
				stop(session, command);
			}
		}
		
		// Okay, we now know that the media must be started.
		NowPlaying started = startInternal(session, command);
		if (started!=null) {
			started.setSoundType(command.soundType);
			if (!nowPlaying.containsKey(session)) {
				nowPlaying.put(session, new java.util.ArrayList<>());
			}
			nowPlaying.get(session).add(started);
		}
	}

	//-------------------------------------------------------------------
	public abstract void muteInternal(NowPlaying item);
	public abstract void unmuteInternal(NowPlaying item);

	//-------------------------------------------------------------------
	public void mute(MUDSession session) {
		if (!nowPlaying.containsKey(session)) return;
		for (NowPlaying item : nowPlaying.get(session)) {
			muteInternal(item);
		}
	}

	//-------------------------------------------------------------------
	public void unmute(MUDSession session) {
		if (!nowPlaying.containsKey(session)) return;
		for (NowPlaying item : nowPlaying.get(session)) {
			unmuteInternal(item);
		}
	}

	//-------------------------------------------------------------------
	public void close(MUDSession session) {
		if (!nowPlaying.containsKey(session)) return;
		for (NowPlaying item : nowPlaying.get(session)) {
			stopInternal(session, null, item);
		}
		nowPlaying.remove(session);
	}

}
