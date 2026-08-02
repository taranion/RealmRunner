module org.prelle.jeditermfxterminal {
	exports org.prelle.ghostty;

	requires javafx.base;
	requires javafx.controls;
	requires transitive javafx.graphics;
	requires java.desktop;
	requires transitive org.prelle.libterminal;
	requires ghosttyfx;
	requires transitive  org.prelle.libansi;
	requires lombok;
}