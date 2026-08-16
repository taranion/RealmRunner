module org.prelle.libterminal {
	exports org.prelle.terminal;
	requires transitive org.prelle.libansi;
	requires lombok;
	requires org.prelle.mudevents;

	uses org.prelle.terminal.TerminalEmulator;
	
}