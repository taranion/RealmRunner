package org.prelle.mudclient.terminal;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;

import org.prelle.mudclient.network.SoundManager;

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
	/**
	 * @see org.prelle.mudclient.network.SoundManager#playMP3(java.nio.file.Path, int)
	 */
	@Override
	public void playMP3(Path file, int volume) {
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

}
