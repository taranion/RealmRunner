module org.prelle.fxterminal {
	exports org.prelle.fxterminal;

	requires javafx.base;
	requires javafx.controls;
	requires transitive javafx.graphics;
	requires org.prelle.libterminal.emulated;
}