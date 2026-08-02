package org.prelle.realmrunner.network;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

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
		public String url;
		private transient Path path;
	}

	public static class NowPlaying {
		private String file;
		private Path path;
		private int loops;
	}
	
	private static SoundManager instance;

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
			case 'u': com.url =collect.toString(); break;
			}
		}
	}

	//-------------------------------------------------------------------
	public abstract void playMP3(Path file, int volume);

	//-------------------------------------------------------------------
	public void playWav(Path file, int volume) {
		logger.log(Level.INFO, "playWAV "+file.toAbsolutePath());
		try {
            // Lade die Audio-Datei
            File soundFile = file.toFile();
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);

            // Bereite den Clip vor
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip audioClip = (Clip) AudioSystem.getLine(info);

            // Öffne den Clip
            audioClip.open(audioStream);

            // Control volume
            FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();
            int percent = Math.min(volume, 100);
            float clampedVolume = (((max-min)*percent) / 100) + min;
             gainControl.setValue(clampedVolume);

            // Füge einen Listener hinzu, um das Ende der Wiedergabe zu erkennen
               audioClip.addLineListener(event -> {
                   if (event.getType() == LineEvent.Type.STOP) {
                       audioClip.close();
                   }
               });

            // Play sound
            audioClip.start();

        } catch (UnsupportedAudioFileException e) {
            logger.log(Level.ERROR, "Unsupported audiofile "+file.toAbsolutePath(),e);
        } catch (LineUnavailableException e) {
            logger.log(Level.ERROR, "Line unavailable "+file.toAbsolutePath(),e);
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error loading file "+file.toAbsolutePath(),e);
        }
	}
}
