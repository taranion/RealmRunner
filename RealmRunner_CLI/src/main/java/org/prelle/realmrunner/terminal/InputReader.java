package org.prelle.realmrunner.terminal;

import java.util.function.Consumer;

import org.prelle.telnet.TelnetOutputStream;

/**
 *
 */
public interface InputReader {

	public InputReader configure(TelnetOutputStream out, Consumer<String> lineEntered);

	public void enterCharacterMode();

	public void enterLineMode();

}
