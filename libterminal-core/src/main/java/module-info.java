module org.prelle.libterminal {
	exports org.prelle.terminal;
	requires transitive org.prelle.libansi;
	requires lombok;

	uses org.prelle.terminal.TerminalEmulator;
	
}