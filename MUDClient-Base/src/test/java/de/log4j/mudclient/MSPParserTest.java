package de.log4j.mudclient;

import javax.sound.sampled.AudioSystem;

import org.prelle.mudclient.network.SoundManager;
import org.prelle.mudclient.network.SoundManager.PlayCommand;

public class MSPParserTest {

	public MSPParserTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String msp = "!!SOUND('doors/open.wav' V=30 U='http://www.realmofmagic.org/msp/doors/open.wav')";

		PlayCommand command = SoundManager.convertMSP(msp);
	}

}
