module org.prelle.realmrunner_cli {
	exports org.prelle.mudclient.terminal;

	requires com.sun.jna;
	requires java.desktop;
	requires jlayer;
	requires mud.framework;
	requires org.prelle.gmcp;
	requires org.prelle.libansi;
	requires org.prelle.libterminal;
	requires org.prelle.libterminal.console;
	requires org.prelle.mudansi;
	requires org.prelle.telnet;
	requires org.yaml.snakeyaml;
	requires tileservice.swing;
	requires org.prelle.mud.client.base;
}