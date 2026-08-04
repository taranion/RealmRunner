module org.prelle.mud.client.base {
	exports org.prelle.realmrunner.network;
	exports org.prelle.realmrunner.feature.translate;
	exports org.prelle.realmrunner.feature.tts;

	requires java.desktop;
	requires java.net.http;
	requires org.prelle.gmcp;
	requires transitive org.prelle.libansi;
	requires transitive org.prelle.libterminal;
	requires org.prelle.telnet;
	requires org.yaml.snakeyaml;
	requires lombok;
	requires org.prelle.mudansi;
	requires jakarta.websocket.client;
	requires org.prelle.libmxp;
	requires langchain4j.core;
	requires langchain4j.ollama;

}