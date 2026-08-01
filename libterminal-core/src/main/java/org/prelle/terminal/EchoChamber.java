package org.prelle.terminal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 */
public class EchoChamber extends FilterOutputStream {
	
	private SwitchableInputStream echoOut;
	private boolean echoEnabled = true;

	//-------------------------------------------------------------------
	/**
	 * @param in
	 */
	public EchoChamber(OutputStream in, SwitchableInputStream echoOut) {
		super(in);
		this.echoOut= echoOut;
	}

	
	
	//-------------------------------------------------------------------
	/**
	 * @see java.io.FilterOutputStream#write(int)
	 */
	@Override
    public void write(int value) throws IOException {
		System.out.println("EchoChamber.write(int) called with "+value+" - echo enabled: "+echoEnabled);
		if (echoEnabled)
			echoOut.inject(value);
		out.write(value);
    }

	//-------------------------------------------------------------------
	/**
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	@Override
    public void write(byte[] b, int off, int len) throws IOException {
		System.out.println("EchoChamber.write(byte[], int, int) called with "+len+" bytes - echo enabled: "+echoEnabled);
         if (echoEnabled)
        	echoOut.inject(b, off, len);
        out.write(b, off, len);
    }

    //-------------------------------------------------------------------
    /**
     * @see java.io.FilterOutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException {
    	System.out.println("EchoChamber.write(byte[]) called with "+b.length+" bytes - echo enabled: "+echoEnabled);
    	 if (echoEnabled)
    		 echoOut.inject(b);
    	 out.write(b, 0, b.length);
    }

	//-------------------------------------------------------------------
	/**
	 * @return the echoEnabled
	 */
	public boolean isEchoEnabled() {
		return echoEnabled;
	}

	//-------------------------------------------------------------------
	/**
	 * @param echoEnabled the echoEnabled to set
	 */
	public void setEchoEnabled(boolean echoEnabled) {
		this.echoEnabled = echoEnabled;
	}

}
