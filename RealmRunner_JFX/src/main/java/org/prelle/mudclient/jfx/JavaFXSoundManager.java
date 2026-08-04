package org.prelle.mudclient.jfx;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.SoundManager;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Uses the "javafx.scene.media" package to play sound files.
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
	 * @see org.prelle.realmrunner.network.SoundManager#play(org.prelle.realmrunner.network.MUDSession, org.prelle.realmrunner.network.SoundManager.PlayCommand)
	 */
	@Override
	protected NowPlaying startInternal(MUDSession session, PlayCommand command) {
		logger.log(Level.WARNING, "Play {0} ",command.path.toUri());
		
		// Check if the file is already playing
		
		
		Media media = new Media(command.path.toUri().toString());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setVolume(command.volume/100.0);
		mediaPlayer.play();
		mediaPlayer.setOnError(new Runnable() {
            public void run() {
                // Handle asynchronous error in MediaPlayer object.
            	logger.log(Logger.Level.ERROR, "Error playing media: " + mediaPlayer.getError().getMessage());
            }
        });
		
		NowPlaying nowPlayingItem = new NowPlaying();
		nowPlayingItem.setId(command.filename);
		nowPlayingItem.setPath(command.path);
		nowPlayingItem.setRemainingLoops(command.loops);
		nowPlayingItem.setOriginalVolume(command.volume);
		nowPlayingItem.setMediaPlayerObject(mediaPlayer);
		return nowPlayingItem;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.SoundManager#stopInternal(org.prelle.realmrunner.network.MUDSession, org.prelle.realmrunner.network.SoundManager.PlayCommand, org.prelle.realmrunner.network.SoundManager.NowPlaying)
	 */
	@Override
	public void stopInternal(MUDSession session, PlayCommand command, NowPlaying playing) {
		// TODO Auto-generated method stub
		logger.log(Level.WARNING, "NOT IMPLEMENTED: Stop {0} ",command.path.toUri());
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.SoundManager#muteInternal(org.prelle.realmrunner.network.SoundManager.NowPlaying)
	 */
	@Override
	public void muteInternal(NowPlaying item) {
		MediaPlayer player = (MediaPlayer) item.getMediaPlayerObject();
		player.setVolume(0.0);
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.realmrunner.network.SoundManager#unmuteInternal(org.prelle.realmrunner.network.SoundManager.NowPlaying)
	 */
	@Override
	public void unmuteInternal(NowPlaying item) {
		MediaPlayer player = (MediaPlayer) item.getMediaPlayerObject();
		player.setVolume(100/item.getOriginalVolume());
	}

}
