package org.prelle.realmrunner.network;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * 
 */
public class RRLogger implements Logger {
	
	public final static Path LOGFILE = MainConfig.CONFIG_DIR.resolve("logfile.txt");
	private static PrintWriter LOGWRITER;
	
	static {
		try {
			LOGWRITER = new PrintWriter(new FileWriter(LOGFILE.toFile()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private final String name;

	//-------------------------------------------------------------------
	public RRLogger(String name) {
		 this.name = name;
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.System.Logger#getName()
	 */
	@Override
	public String getName() {
		return  name;
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.System.Logger#isLoggable(java.lang.System.Logger.Level)
	 */
	@Override
	public boolean isLoggable(Level level) {
		if (name.startsWith("java")) return false;
		if (name.startsWith("jdk.")) return false;
		return level.getSeverity() >= Level.DEBUG.getSeverity();
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.System.Logger#log(java.lang.System.Logger.Level, java.util.ResourceBundle, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		 if (isLoggable(level)) {
			 LOGWRITER.write(msg+"\r\n");
	         thrown.printStackTrace(LOGWRITER);
	         LOGWRITER.flush();
	     }
	}

	//-------------------------------------------------------------------
	/**
	 * @see java.lang.System.Logger#log(java.lang.System.Logger.Level, java.util.ResourceBundle, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		if (isLoggable(level)) {
			if (bundle!=null)
				format = bundle.getString(format);
			String formatted = MessageFormat.format(format, params);
			LOGWRITER.format("%5s [%18s]: %s\r\n", level, name, formatted);
			LOGWRITER.flush();
        }
	}

}
