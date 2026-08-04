package org.prelle.realmrunner.terminal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.SoundManager;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

/**
 *
 */
public class JLayerSoundManager extends SoundManager {

	private final static Logger logger = System.getLogger("mud.sound");

	//-------------------------------------------------------------------
	/**
	 */
	public JLayerSoundManager() {
	}

	//-------------------------------------------------------------------
	private void playWav(Path file, int volume) {
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

	//-------------------------------------------------------------------
	private void playMP3(Path file, int volume) {
		logger.log(Level.INFO, "playMP3 "+file.toAbsolutePath());
		try {
			if (file!=null) {
				Player player = new Player(new FileInputStream(file.toFile()));
				Thread thread = new Thread( () -> {
					try {
						player.play();
					} catch (JavaLayerException e) {
						logger.log(Level.ERROR, "Error playing "+file,e);
						e.printStackTrace();
					}
				});
				thread.start();
			}
		} catch (FileNotFoundException e) {
			logger.log(Level.ERROR, "Error playing "+file,e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JavaLayerException e) {
			logger.log(Level.ERROR, "Error playing "+file,e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stopInternal(MUDSession session, PlayCommand command, NowPlaying playing) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected NowPlaying startInternal(MUDSession session, PlayCommand command) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void muteInternal(NowPlaying item) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unmuteInternal(NowPlaying item) {
		// TODO Auto-generated method stub
		
	}

}
