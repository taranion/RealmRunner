module org.prelle.jeditermfxterminal {
	exports org.prelle.jeditermfxterminal;

	requires javafx.base;
	requires javafx.controls;
	requires transitive javafx.graphics;
	requires java.desktop;
	requires org.prelle.libterminal;
	requires ghosttyfx;
	requires org.prelle.libansi;
}