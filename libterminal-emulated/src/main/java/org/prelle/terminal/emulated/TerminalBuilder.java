package org.prelle.terminal.emulated;

import org.prelle.ansi.ANSIInputStream;
import org.prelle.terminal.emulated.delete.ActiveTerminalController;
import org.prelle.terminal.emulated.delete.Emulation;
import org.prelle.terminal.emulated.delete.ITerminalView;
import org.prelle.terminal.emulated.delete.PassiveController;

/**
 *
 */
public class TerminalBuilder {

	private ITerminalView view;
	private Terminal.Size size;
	private Emulation emulation;
	private ANSIInputStream active;

	//-------------------------------------------------------------------
	public TerminalBuilder() {
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	public TerminalBuilder withView(ITerminalView value) {
		this.view = value;
		return this;
	}

	//-------------------------------------------------------------------
	public TerminalBuilder withSize(Terminal.Size value) {
		this.size = value;
		return this;
	}

	//-------------------------------------------------------------------
	public TerminalBuilder emulate(Emulation value) {
		this.emulation = value;
		return this;
	}

	//-------------------------------------------------------------------
	public Terminal buildPassive() {
		TerminalModel model = new TerminalModel();
		TerminalController ctrl = new PassiveController(model, emulation);
		return new Terminal(model, view, ctrl, size);
	}

	//-------------------------------------------------------------------
	public Terminal buildActive(ANSIInputStream in) {
		TerminalModel model = new TerminalModel();
		TerminalController ctrl = new ActiveTerminalController(model, emulation, in);
		return new Terminal(model, view, ctrl, size);
	}

}
