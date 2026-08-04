package org.prelle.terminal;

import java.util.Locale;

/**
 * Fallback no-op implementation of TTSEngine when no real speech engine is configured.
 */
public class NoOpTTSEngine implements TTSEngine {

	@Override
	public void speak(String text, Locale language, String voice) throws Exception {
		// No-op fallback
	}
}
