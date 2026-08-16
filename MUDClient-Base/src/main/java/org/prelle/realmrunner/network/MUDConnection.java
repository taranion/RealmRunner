package org.prelle.realmrunner.network;

import org.prelle.mudevents.MUDEventPipeline;
import org.prelle.mudevents.MUDEventProcessor;

import lombok.Getter;

/**
 * 
 */
public abstract class MUDConnection implements MUDEventProcessor{
	
	protected MUDEventPipeline receivePipe;
	@Getter protected boolean supportsTelnet;
	@Getter protected boolean supportsMUDDown;
	
	//-------------------------------------------------------------------
	protected MUDConnection() {
		receivePipe = new MUDEventPipeline("RCV");
	}
	
	//-------------------------------------------------------------------
	public MUDEventPipeline getReceivePipe() {
		return receivePipe;
	}

	public abstract void start();
	
	public abstract void close();
	
}
