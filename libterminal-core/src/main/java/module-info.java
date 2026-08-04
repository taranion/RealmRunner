module org.prelle.libterminal {
	exports org.prelle.terminal;
	requires transitive org.prelle.libansi;
	requires lombok;
	requires langchain4j.core;
	requires langchain4j.ollama;

	uses org.prelle.terminal.TerminalEmulator;
	
}