package org.prelle.realmrunner.network;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class RRLoggerFinder extends LoggerFinder {

	private static final Map<String, RRLogger> LOGGERS = new HashMap<>();
	
	//-------------------------------------------------------------------
	public RRLoggerFinder() {
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.System.LoggerFinder#getLogger(java.lang.String, java.lang.Module)
	 */
	@Override
	public Logger getLogger(String name, Module module) {
		return LOGGERS.computeIfAbsent(name, RRLogger::new); 
	}

}
