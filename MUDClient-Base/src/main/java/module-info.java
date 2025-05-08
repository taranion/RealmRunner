module org.prelle.mud.client.base {
	exports org.prelle.realmrunner.network;

	requires java.desktop;
	requires java.net.http;
	requires org.prelle.gmcp;
	requires transitive org.prelle.libansi;
	requires org.prelle.libterminal;
	requires org.prelle.telnet;
	requires org.yaml.snakeyaml;
	requires lombok;
	
}