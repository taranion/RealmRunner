package org.prelle.realmrunner.feature.tts;

import java.util.Locale;

/**
 * Pluggable interface for Text-To-Speech (TTS) synthesis and audio playback engines.
 * Implementations can wrap local OS speech engines or external API SDKs
 * (e.g. Gemini Expressive TTS, System Speech, etc.).
 */
public interface TTSEngine {

	/**
	 * Speaks the given plain text string directly using the specified language and voice.
	 *
	 * @param text Plain text to speak
	 * @param language Locale indicating output language
	 * @param voice Voice identifier or name (optional, may be null for default)
	 * @throws Exception If synthesis or audio playback fails
	 */
	void speak(String text, Locale language, String voice) throws Exception;

	/**
	 * Convenience overload to speak text using default system language and voice.
	 *
	 * @param text Plain text to speak
	 * @throws Exception If synthesis or audio playback fails
	 */
	default void speak(String text) throws Exception {
		speak(text, Locale.getDefault(), null);
	}

	/**
	 * Synthesizes the given plain text string into raw audio bytes (e.g. WAV, MP3).
	 * Optional method for engines that support file or cache persistence.
	 *
	 * @param text Plain text to synthesize
	 * @param language Locale indicating output language
	 * @param voice Voice identifier or name (optional, may be null for default)
	 * @return Raw audio bytes, or null if direct byte synthesis is not supported
	 * @throws Exception If synthesis fails
	 */
	default byte[] synthesize(String text, Locale language, String voice) throws Exception {
		return synthesize(text);
	}

	/**
	 * Convenience overload to synthesize text using default system language and voice.
	 *
	 * @param text Plain text to synthesize
	 * @return Raw audio bytes, or null if direct byte synthesis is not supported
	 * @throws Exception If synthesis fails
	 */
	default byte[] synthesize(String text) throws Exception {
		return null;
	}

	/**
	 * Plays pre-synthesized raw audio bytes.
	 *
	 * @param audioBytes Raw audio bytes to play
	 * @throws Exception If playback fails
	 */
	default void playAudio(byte[] audioBytes) throws Exception {}

	/**
	 * Stops any active or queued speech audio output.
	 */
	default void stop() {}

	/**
	 * Returns whether the engine is currently speaking or synthesizing speech.
	 *
	 * @return true if active, false otherwise
	 */
	default boolean isSpeaking() {
		return false;
	}

	/**
	 * Returns whether the engine can synthesize plain text into raw audio data (e.g. WAV, MP3).
	 *
	 * @return true if byte synthesis is supported, false otherwise
	 */
	default boolean canSynthesize() {
		return false;
	}

	/**
	 * Returns whether the engine can speak plain text directly.
	 *
	 * @return true if direct speech output is supported, false otherwise
	 */
	default boolean canSpeak() {
		return true;
	}

	/**
	 * Releases any underlying resources held by the TTS engine.
	 */
	default void close() {}
}
