package de.log4j.mudclient;

import org.prelle.realmrunner.network.MSPHandler;
import org.prelle.realmrunner.network.SoundManager.PlayCommand;
import org.prelle.realmrunner.network.SoundManager.SoundType;

public class MSPParserTest {

	public MSPParserTest() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String[] examples = {
			"audio/01_Ultima_Theme.mp3 V=75 L=-1 C=1 U=http://eden-test.rpgframework.de:4080",
			"audio/01_Ultima_Theme.mp3 V=75 L =-1 C= 1 U = http://eden-test.rpgframework.de:4080 ",
			"thunder V=100 L=1 P=30 T=weather",
			"weather/rain.wav V=80 P=20 T=weather",
			"alarm*.wav P=100 T=utility",
			"Off",
			"Off U=http://www.example.org:5000/sounds",
			"fugue.mid V=100 L=1 C=1 T=music U=http://www.example.net/",
			"berlioz/fantas? V=80 L=-1 C=1 T=music",
			"Off"
		};

		for (String ex : examples) {
			PlayCommand cmd = MSPHandler.parse(SoundType.SOUND, ex);
			System.out.println("Line: " + ex);
			System.out.println(" -> fn=" + cmd.filename + ", V=" + cmd.volume + ", L=" + cmd.loops + ", P=" + cmd.priority + ", C=" + cmd.cont + ", T=" + cmd.type + ", U=" + cmd.url);
		}
	}

}
