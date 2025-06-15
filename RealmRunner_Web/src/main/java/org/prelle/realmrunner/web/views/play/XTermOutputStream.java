package org.prelle.realmrunner.web.views.play;

import java.io.IOException;
import java.io.OutputStream;

import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * 
 */
public class XTermOutputStream extends OutputStream {
	
	private XTerm term;
	private UI ui;

	//-------------------------------------------------------------------
	/**
	 */
	public XTermOutputStream(XTerm xterm) {
		this.term = xterm;
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.FilterOutputStream#write(int)
	 */
	public synchronized void write(int value) throws IOException {
		String str = new String(new byte[] {(byte) value});
		if (ui!=null) {
//			System.err.println("Wrote1a: "+value+" in UI "+ui);
			ui.access( () -> {
				term.write(str);
			});
		} else if (VaadinSession.getCurrent()!=null) {
//			System.err.println("Wrote1b: "+value+" in session "+VaadinSession.getCurrent());
			term.write(str);
		} else {
//			System.err.println("Wrote1c: "+value+" without UI or session");
			term.write(str);
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.FilterOutputStream#write(byte[])
	 */
	public synchronized void write(byte[] values) throws IOException {
		String str = new String(values);
		if (VaadinSession.getCurrent()!=null) {
//			System.err.println("Wrote2b: "+str+" in session "+VaadinSession.getCurrent());
			term.write(str);
			term.focus();
		} else if (ui!=null) {
//			System.err.println("Wrote2a: "+str+" without session but in UI "+ui);
			ui.access( () -> {
				term.write(str);
				term.focus();
			});
		} else {
//			System.err.println("Wrote2c: "+str+" without UI or session");
			term.write(str);
		}
	}

	//-------------------------------------------------------------------
	public synchronized void write(String value) throws IOException {
		if (ui!=null) {
			System.err.println("Wrote3a: "+value+" in UI "+ui);
			ui.access( () -> {
				term.write(value);
			});
		} else if (VaadinSession.getCurrent()!=null) {
			System.err.println("Wrote3b: "+value+" in session "+VaadinSession.getCurrent());
			term.write(value);
		} else {
			System.err.println("Wrote3c: "+value+" without UI or session");
			term.write(value);
		}
	}

	//-------------------------------------------------------------------
	public void setUI(UI ui) {
		this.ui = ui;
	}

}
