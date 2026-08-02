package org.prelle.mudclient.jfx;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.prelle.realmrunner.network.SoundManager;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * 
 */
public class JavaFXSoundManager extends SoundManager {

	//-------------------------------------------------------------------
	/**
	 */
	public JavaFXSoundManager() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.SoundManager#playMP3(java.nio.file.Path, int)
	 */
	@Override
	public void playMP3(Path file, int volume) {
		logger.log(Level.WARNING, "Play {0} ",file.toUri());
		Media media = new Media(file.toUri().toString());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setVolume(volume/100.0);
		mediaPlayer.play();
		mediaPlayer.setOnError(new Runnable() {
            public void run() {
                // Handle asynchronous error in MediaPlayer object.
            	logger.log(Logger.Level.ERROR, "Error playing media: " + mediaPlayer.getError().getMessage());
            }
        });
	}

}
