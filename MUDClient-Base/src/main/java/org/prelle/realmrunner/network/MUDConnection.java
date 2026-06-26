package org.prelle.realmrunner.network;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 */
public interface MUDConnection {

	public InputStream getStreamFromMUD();
	
	public OutputStream getStreamToMUD();
}
