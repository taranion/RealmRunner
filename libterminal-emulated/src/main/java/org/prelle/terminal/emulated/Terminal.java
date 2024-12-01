package org.prelle.terminal.emulated;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.prelle.terminal.emulated.delete.ITerminalView;
import org.prelle.terminal.emulated.delete.ITerminalViewListener;
import org.prelle.terminal.emulated.delete.PassiveController;

/**
 *
 */
public class Terminal {

	public static enum Size {
		VARIABLE,
		FIXED_80x24,
		FIXED_80x25,
		FIXED_132x24,
	}


	private TerminalModel model;
	private ITerminalView view;
	private TerminalController controller;
	private Size size;

	//-------------------------------------------------------------------
	public static TerminalBuilder builder() {
		return new TerminalBuilder();
	}

	//-------------------------------------------------------------------
	Terminal(TerminalModel model, ITerminalView view, TerminalController ctrl, Terminal.Size size) {
		this.model = model;
		this.view  = view;
		this.controller = ctrl;
		this.size  = size;

		model.setTerminalView(view);
	}

	//-------------------------------------------------------------------
	public ITerminalViewListener getViewListener() {
		return controller;
	}

	//-------------------------------------------------------------------
	public void write(String text) {
		if (controller instanceof PassiveController) {
			((PassiveController)controller).write(text);
		} else
			throw new IllegalStateException("write() may only be used on passive controller");
	}

	//-------------------------------------------------------------------
	public void setView(ITerminalView view) {
		this.view = view;
		model.setTerminalView(view);
		view.addTerminalListener(controller);
	}

	//-------------------------------------------------------------------
	public TerminalModel getModel() {
		return model;
	}

}
