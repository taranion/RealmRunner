package org.prelle.terminal.emulated;

/**
 * Implement this interview to get your visualization
 */
public interface ITerminalView {

	//-------------------------------------------------------------------
	/**
	 * Reset the entire rendering
	 */
	public void clear();

	public void update(int x, int y, CharInfo glyph);

	public void addTerminalListener(ITerminalViewListener callback);

}
