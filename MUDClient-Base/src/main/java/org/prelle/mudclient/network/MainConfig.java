package org.prelle.mudclient.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;

/**
 *
 */
@Getter
@Setter
public class MainConfig extends AbstractConfig {

	private Map<String, Config> world = new LinkedHashMap<>();

	//-------------------------------------------------------------------
	public MainConfig() {
		this.setMusic(true);
		this.setSound(true);
		this.setServerLayoutControl(false);
		this.setLocalEcho(true);
	}

	//-------------------------------------------------------------------
	public void addWorld(String name, Config config) {
		world.put(name, config);
	}

	//-------------------------------------------------------------------
	public List<Entry<String,Config>> getWorlds() {
		return new ArrayList<>(world.entrySet());
	}

}
