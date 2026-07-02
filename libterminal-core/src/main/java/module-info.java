module org.prelle.libterminal {
	exports org.prelle.terminal;
	requires transitive org.prelle.libansi;

	uses org.prelle.terminal.TerminalEmulator;
	
}