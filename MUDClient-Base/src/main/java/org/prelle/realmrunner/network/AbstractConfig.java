package org.prelle.realmrunner.network;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter
@Setter
public class AbstractConfig {

	/**
	 * If TRUE, allow the server to control horizontal or vertical splitting.
	 * If FALSE, the client will provide a split screen for input and output
	 */
	private Boolean serverLayoutControl;
	private Boolean localEcho;
	/**
	 * Directory where to store files on a per-world basis.
	 *
	 */
	private String dataDir;
	/**
	 * If TRUE the media player will play background music, should the MUD provide it (GMCP, MSP)
	 */
	private Boolean music;
	/**
	 * If TRUE the media player will play sound files (GMCP,MSP), should the MUD provide it
	 */
	private Boolean sound;
	/**
	 * If a character is received from the MUD, don't wait for an GA to flush
	 * the buffer, but flush after every character. Relevant for prompts not
	 * ending with a newline.
	 */
	private Boolean missingGAWorkaround;
	private Boolean sendCRbeforeLF;
	/**
	 * Shall codes passed by the terminal to the MUD be sent?
	 */
	private Boolean ignoreControlCodesFromTerminal;

	//-------------------------------------------------------------------
	public AbstractConfig() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	public AbstractConfig(AbstractConfig copy) {
		this.ignoreControlCodesFromTerminal = copy.ignoreControlCodesFromTerminal;
		this.localEcho = copy.localEcho;
		this.missingGAWorkaround = copy.missingGAWorkaround;
		this.music = copy.music;
		this.sendCRbeforeLF = copy.sendCRbeforeLF;
		this.serverLayoutControl = copy.serverLayoutControl;
		this.sound = copy.sound;
	}

}
