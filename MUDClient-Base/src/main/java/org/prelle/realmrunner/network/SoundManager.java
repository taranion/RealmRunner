package org.prelle.realmrunner.network;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
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

import org.prelle.mud4j.gmcp.Client.ClientMediaPlay;
import org.prelle.telnet.mud.MUDSoundProtocolListener;

/**
 *
 */
public abstract class SoundManager implements MUDSoundProtocolListener {

	private final static Logger logger = System.getLogger("mud.sound");

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

	//-------------------------------------------------------------------
	/**
	 */
	public SoundManager() {
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.telnet.mud.MUDSoundProtocolListener#mspReceivedCommand(java.lang.String)
	 */
	@Override
	public void mspReceivedCommand(String mspCommand) {
		logger.log(Level.WARNING, "TODO: "+mspCommand);
		PlayCommand com = convertMSP(mspCommand);
		logger.log(Level.WARNING, "com = "+com);
		try {
			if (com.url!=null) {
				logger.log(Level.WARNING, "URI ="+com.url);
				URI uri = URI.create(com.url);
				Path result = DataFileManager.downloadFileTo(com.filename, uri);
				if (result.getFileName().toString().endsWith(".mp3"))
					playMP3(result, com.volume);
				else
					playWav(result, com.volume);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------
	public static PlayCommand convertMSP(String mspLine) {
		PlayCommand com = new PlayCommand();
		if (mspLine.startsWith("!!SOUND(")) {
			com.soundType=SoundType.SOUND;
		} else if (mspLine.startsWith("!!MUSIC(")) {
			com.soundType=SoundType.MUSIC;
		} else
			throw new IllegalArgumentException("Not a valid MSP line: "+mspLine);
		String data = mspLine.substring(8, mspLine.length()-2);

		Character enclosedBy = null;
		StringBuffer collect = new StringBuffer();
		Character currentType = null;
		for (int i=0; i<data.length(); i++) {
			char c=data.charAt(i);
			switch (c) {
			case '\'': case '"':
				if (enclosedBy==null) {
					enclosedBy=c;
					continue;
				} else if (enclosedBy==c) {
					// Ends
					enclosedBy=null;
					assignTo(com, currentType, collect);
					currentType=null;
					collect.delete(0, collect.length());
				}
				break;
			case ')':
				break;
			case ' ':
				if (collect.length()==0)
					continue;
				logger.log(Level.DEBUG, "Parse type {0} = {1}", currentType, collect);
				assignTo(com, currentType, collect);
				currentType=null;
				collect.delete(0, collect.length());
				break;
			case '=':
				currentType=collect.charAt(0);
				collect.delete(0, collect.length());
				break;
			default:
				collect.append(c);
			}
		}
		if (collect.length()>0) {
			assignTo(com, currentType, collect);
		}

		return com;
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
