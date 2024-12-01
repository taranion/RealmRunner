package org.prelle.mudansi;

import java.io.IOException;
import java.util.List;

import org.prelle.ansi.ANSIOutputStream;

/**
 *
 */
public interface UserInterfaceFormat {

	//-------------------------------------------------------------------
	public void initialize(ANSIOutputStream out) throws IOException;

	//-------------------------------------------------------------------
	public void sendRoomDescription(ANSIOutputStream out, List<String> lines) throws IOException;


}
