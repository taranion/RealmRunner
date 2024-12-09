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
	
	private String name;
	private Level minLevel;

	//-------------------------------------------------------------------
	public RRLogger(String name, Level minLevel) {
		 this.name = name;
		 this.minLevel = minLevel;
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
		return level.getSeverity()>=minLevel.getSeverity();
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
			
			String formatted = format;
			try {
				formatted = MessageFormat.format(format, params);
			} catch (IllegalArgumentException e) {}
			String prefix = "";
			try {
				throw new RuntimeException("trace");
			} catch (Exception e) {
				StackTraceElement element = e.getStackTrace()[2];
				if (element.getClassName().equals("de.rpgframework.MultiLanguageResourceBundle"))
					element = e.getStackTrace()[5];
				prefix="("+element.getClassName().substring(element.getClassName().lastIndexOf(".")+1)+".java:"+element.getLineNumber()+") : ";
			}
			try {
				LOGWRITER.format("%5s [%18s] (%s): %s\r\n", level, name, prefix,formatted);
			} catch (Exception e) {
				LOGWRITER.write(format+"\n");
				e.printStackTrace(LOGWRITER);
			}
			LOGWRITER.flush();
        }
	}

}
