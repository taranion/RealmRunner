module org.prelle.realmrunner_cli {
	exports org.prelle.realmrunner.terminal;

	requires java.desktop;
	requires jlayer;
	requires org.prelle.gmcp;
	requires org.prelle.libansi;
	requires org.prelle.libterminal;
	requires org.prelle.libterminal.console;
	requires org.prelle.mudansi;
	requires org.prelle.telnet;
	requires org.yaml.snakeyaml;
	requires org.prelle.mud.client.base;
	requires graphicmud.core;
	requires graphicmud.tiles.jfx;
	requires org.graalvm.nativeimage;
	
	uses java.lang.System.LoggerFinder;
}