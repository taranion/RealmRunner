package org.prelle.realmrunner.web.views.play;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.prelle.ansi.C0Code;

import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.UI;

/**
 * 
 */
public class XTermInputStream extends InputStream {
	
	private XTerm term;
	private UI ui;
	
	private List<Integer> buffer = new ArrayList<>();

	//-------------------------------------------------------------------
	/**
	 */
	public XTermInputStream(XTerm xterm) {
		this.term = xterm;
		term.addLineListener(le -> {
			System.err.println("Line "+le.getLine());
			synchronized (buffer) {
				le.getLine().chars().forEach(i -> buffer.add(i));
				buffer.add(C0Code.CR.code());
				buffer.add(C0Code.LF.code());
				buffer.notify();
			}
		});
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		synchronized (buffer) {
			while (buffer.isEmpty()) {
				try {
					buffer.wait(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (buffer.isEmpty())
				return -1;
			return buffer.removeFirst();
		}
	}

}
