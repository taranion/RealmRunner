module org.prelle.libterminal.emulated {
	exports org.prelle.terminal.emulated;
	exports org.prelle.terminal.emulated.delete;

	requires transitive org.prelle.libansi;
	requires org.prelle.libterminal;
}