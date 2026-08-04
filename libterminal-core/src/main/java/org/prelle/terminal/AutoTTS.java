package org.prelle.terminal;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.prelle.terminal.ReceiveBuffer.ReadBufferHandler;
import org.prelle.terminal.ReceiveBuffer.ReceivedLine;

import lombok.Getter;
import lombok.Setter;

/**
 * ReadBufferHandler implementation that filters incoming terminal text (ignoring ASCII art and blank lines)
 * and sends it to a pluggable TTSEngine for speech synthesis with configurable Locale language and voice.
 * Supports SHA-256 digest caching, LocalDateTime aging, LRU eviction, and individual audio file persistence.
 */
public class AutoTTS implements ReadBufferHandler {

	private final static Logger logger = System.getLogger("terminal");

	private static final int DEFAULT_MAX_CACHE_SIZE = 5000;

	/**
	 * Value object representing a cached speech entry with last access timestamp and optional audio file path.
	 */
	@Getter
	@Setter
	public static class TTSEntry {
		private String text;
		private LocalDateTime lastAccessed;
		private Path audioFile;

		public TTSEntry(String text) {
			this(text, LocalDateTime.now(), null);
		}

		public TTSEntry(String text, LocalDateTime lastAccessed, Path audioFile) {
			this.text = text;
			this.lastAccessed = lastAccessed != null ? lastAccessed : LocalDateTime.now();
			this.audioFile = audioFile;
		}

		public void touch() {
			this.lastAccessed = LocalDateTime.now();
		}
	}

	@Getter @Setter
	private TTSEngine engine;

	@Getter @Setter
	private Locale language = Locale.getDefault();

	@Getter @Setter
	private String voice;

	@Getter @Setter
	private int maxCacheSize;

	@Getter @Setter
	private double minAlphanumericRatio = 0.4;

	@Getter @Setter
	private Path storageDirectory;

	@Getter @Setter
	private String audioExtension = "wav";

	@Getter
	private final Map<String, TTSEntry> cache;

	private final ExecutorService executorService;

	//-------------------------------------------------------------------
	/**
	 * Default constructor using a NoOpTTSEngine fallback and system default language.
	 */
	public AutoTTS() {
		this(new NoOpTTSEngine(), Locale.getDefault(), null, DEFAULT_MAX_CACHE_SIZE);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor with a specific TTSEngine.
	 */
	public AutoTTS(TTSEngine engine) {
		this(engine, Locale.getDefault(), null, DEFAULT_MAX_CACHE_SIZE);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor with a specific TTSEngine, target Locale language, and voice.
	 */
	public AutoTTS(TTSEngine engine, Locale language, String voice) {
		this(engine, language, voice, DEFAULT_MAX_CACHE_SIZE);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor specifying TTSEngine, target Locale language, voice, and max cache size.
	 */
	public AutoTTS(TTSEngine engine, Locale language, String voice, int maxCacheSize) {
		this.engine = engine != null ? engine : new NoOpTTSEngine();
		this.language = language != null ? language : Locale.getDefault();
		this.voice = voice;
		this.maxCacheSize = maxCacheSize;
		this.executorService = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "AutoTTS-SpeechThread");
			t.setDaemon(true);
			return t;
		});
		this.cache = Collections.synchronizedMap(
			new LinkedHashMap<String, TTSEntry>(16, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, TTSEntry> eldest) {
					boolean remove = size() > AutoTTS.this.maxCacheSize;
					if (remove && eldest.getValue() != null && eldest.getValue().getAudioFile() != null) {
						try {
							Files.deleteIfExists(eldest.getValue().getAudioFile());
						} catch (IOException e) {
							logger.log(Level.WARNING, "Failed deleting evicted audio file: " + eldest.getValue().getAudioFile(), e);
						}
					}
					return remove;
				}
			}
		);
	}

	//-------------------------------------------------------------------
	public void clearCache() {
		cache.clear();
	}

	//-------------------------------------------------------------------
	public String computeDigest(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder(2 * hash.length);
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			return Integer.toHexString(text.hashCode());
		}
	}

	//-------------------------------------------------------------------
	/**
	 * Determines if text should be treated as ASCII art or line graphics to prevent speaking it.
	 */
	public boolean isAsciiArt(String text) {
		if (text == null || text.trim().isEmpty()) {
			return false;
		}

		int alphaNumericCount = 0;
		int totalNonWhitespaceCount = 0;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				continue;
			}
			totalNonWhitespaceCount++;
			if (Character.isLetterOrDigit(c)) {
				alphaNumericCount++;
			}
		}

		if (totalNonWhitespaceCount == 0) {
			return false;
		}

		double ratio = (double) alphaNumericCount / totalNonWhitespaceCount;
		return ratio < minAlphanumericRatio;
	}

	//-------------------------------------------------------------------
	/**
	 * Helper to get the individual audio file path for a text block on disk, scoped by language and voice.
	 */
	public Path getAudioFilePath(String text) {
		if (storageDirectory == null) {
			return null;
		}
		String digest = computeDigest(text);
		return storageDirectory.resolve(digest + "." + audioExtension);
	}

	//-------------------------------------------------------------------
	/**
	 * Persists individual audio bytes to disk under the storage directory using SHA-256 digest.
	 */
	public Path saveAudioFile(String text, byte[] audioData) throws IOException {
		if (storageDirectory == null || audioData == null) {
			return null;
		}
		if (!Files.exists(storageDirectory)) {
			Files.createDirectories(storageDirectory);
		}
		Path filePath = getAudioFilePath(text);
		Files.write(filePath, audioData);
		return filePath;
	}

	//-------------------------------------------------------------------
	/**
	 * Loads individual audio bytes from disk if available for the given text.
	 */
	public byte[] loadAudioFile(String text) throws IOException {
		Path filePath = getAudioFilePath(text);
		if (filePath != null && Files.exists(filePath) && Files.isRegularFile(filePath)) {
			return Files.readAllBytes(filePath);
		}
		return null;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.ReceiveBuffer.ReadBufferHandler#onLineReceived(org.prelle.terminal.ReceiveBuffer.ReceivedLine, java.util.List)
	 */
	@Override
	public boolean onLineReceived(ReceivedLine line, List<ReceivedLine> history) {
		if (line == null) {
			return false;
		}

		String plainText = line.getOriginalAsText();
		if (plainText == null || plainText.trim().isEmpty()) {
			return false;
		}

		// Filter out ASCII art / line graphics to prevent speaking noise
		if (isAsciiArt(plainText)) {
			logger.log(Level.DEBUG, "AutoTTS skipping ASCII art line: {0}", plainText);
			return false;
		}

		String textToSpeak = plainText.trim();
		String cacheKey = computeDigest(textToSpeak);

		// Asynchronous speech execution to prevent blocking the ReceiveBuffer stream thread
		executorService.submit(() -> {
			try {
				TTSEntry entry = cache.get(cacheKey);
				if (entry != null) {
					entry.touch();
					if (entry.getAudioFile() != null && Files.exists(entry.getAudioFile())) {
						byte[] audioBytes = Files.readAllBytes(entry.getAudioFile());
						engine.playAudio(audioBytes);
						return;
					}
				}

				logger.log(Level.DEBUG, "AutoTTS speaking text: {0} (language={1}, voice={2})", textToSpeak, language, voice);

				// Attempt to synthesize audio bytes first for disk caching, or speak directly
				byte[] synthesizedAudio = engine.synthesize(textToSpeak, language, voice);
				Path savedFile = null;
				if (synthesizedAudio != null && storageDirectory != null) {
					savedFile = saveAudioFile(textToSpeak, synthesizedAudio);
					engine.playAudio(synthesizedAudio);
				} else {
					engine.speak(textToSpeak, language, voice);
				}

				cache.put(cacheKey, new TTSEntry(textToSpeak, LocalDateTime.now(), savedFile));

			} catch (Exception e) {
				logger.log(Level.WARNING, "AutoTTS speech synthesis failed for text: " + textToSpeak, e);
			}
		});

		return false;
	}

	//-------------------------------------------------------------------
	public void stop() {
		if (engine != null) {
			engine.stop();
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.ReceiveBuffer.ReadBufferHandler#onConnectionLost()
	 */
	@Override
	public void onConnectionLost() {
		stop();
	}

	//-------------------------------------------------------------------
	public void shutdown() {
		executorService.shutdownNow();
		if (engine != null) {
			engine.close();
		}
	}
}
