package org.prelle.realmrunner.feature.translate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.PrintableFragment;
import org.prelle.realmrunner.network.DataFileManager;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.terminal.ReceiveBuffer.HandlerResult;
import org.prelle.terminal.ReceiveBuffer.ReadBufferHandler;
import org.prelle.terminal.ReceiveBuffer.ReceivedLine;
import static org.prelle.terminal.ReceiveBuffer.NO_CHANGE;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Getter;
import lombok.Setter;

/**
 * Translation service that receives line objects, preserves non-printable ANSI fragments
 * (such as colors and styling), detects ASCII art / line decorations to avoid translating them,
 * and translates printable text to the target language via ChatModel.
 * Includes SHA-256 digest keys, LocalDateTime access timestamps, LRU cache retention,
 * and file persistence (loadTranslations / saveTranslations).
 */
public class LLMAutoTranslate implements ReadBufferHandler {

	private final static Logger logger = System.getLogger("terminal");
	private final static Pattern TAG_PATTERN = Pattern.compile("<a(\\d+)/>");

	private static final String DEFAULT_BASE_URL = "http://localhost:11434"; 
	private static final int DEFAULT_MAX_CACHE_SIZE = 5000;

	/**
	 * Value object representing a cached translation along with its last access timestamp.
	 */
	@Getter
	@Setter
	public static class TranslationEntry {
		private String translation;
		private LocalDateTime lastAccessed;

		public TranslationEntry(String translation) {
			this(translation, LocalDateTime.now());
		}

		public TranslationEntry(String translation, LocalDateTime lastAccessed) {
			this.translation = translation;
			this.lastAccessed = lastAccessed != null ? lastAccessed : LocalDateTime.now();
		}

		public void touch() {
			this.lastAccessed = LocalDateTime.now();
		}
	}

	@Getter
	private final MUDSession session;

	@Getter
	private String targetLanguage = "de"; 

	@Getter @Setter
	private ChatModel chatModel;

	@Getter @Setter
	private int maxCacheSize;

	@Getter @Setter
	private double minAlphanumericRatio = 0.4;

	private final Map<String, TranslationEntry> cache;
	
	@Setter
	private Path cacheDirectory;

	@Getter @Setter
	private Path cacheFilePath;

	private volatile boolean modified = false;
	private final ScheduledExecutorService saveScheduler;

	//-------------------------------------------------------------------
	/**
	 * Default constructor using Ollama default endpoint (http://localhost:11434) and qwen3:8b model.
	 */
	public LLMAutoTranslate(MUDSession session, Locale locale) {
		this(session, locale, DEFAULT_BASE_URL, "qwen3:8b", DEFAULT_MAX_CACHE_SIZE);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor specifying target language, Ollama URL, and model name.
	 */
	public LLMAutoTranslate(MUDSession session, Locale targetLanguage, String baseUrl, String modelName) {
		this(session, targetLanguage, baseUrl, modelName, DEFAULT_MAX_CACHE_SIZE);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor specifying target language, Ollama URL, model name, and max cache size.
	 */
	public LLMAutoTranslate(MUDSession session, Locale targetLanguage, String baseUrl, String modelName, int maxCacheSize) {
		this(session, OllamaChatModel.builder()
				.baseUrl(baseUrl)
				.modelName(modelName)
				.temperature(0.3)
				.build(), targetLanguage, maxCacheSize);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor with pre-configured ChatModel and target language.
	 */
	public LLMAutoTranslate(MUDSession session, ChatModel chatModel, Locale targetLanguage) {
		this(session, chatModel, targetLanguage, DEFAULT_MAX_CACHE_SIZE);
	}

	//-------------------------------------------------------------------
	/**
	 * Constructor with pre-configured ChatModel, target language, and max cache size.
	 */
	public LLMAutoTranslate(MUDSession session, ChatModel chatModel, Locale locale, int maxCacheSize) {
		if (session == null) {
			throw new IllegalArgumentException("session cannot be null");
		}
		this.session = session;
		this.chatModel = chatModel;
		this.targetLanguage = locale != null ? locale.getLanguage() : "de";
		this.maxCacheSize = maxCacheSize;
		this.cache = Collections.synchronizedMap(
			new LinkedHashMap<String, TranslationEntry>(16, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, TranslationEntry> eldest) {
					return size() > LLMAutoTranslate.this.maxCacheSize;
				}
			}
		);
		
		try {
			this.cacheDirectory = DataFileManager.getCurrentDataDir(session);
			this.cacheFilePath  = cacheDirectory.resolve("translation_cache_" + this.targetLanguage + ".properties");
			if (Files.exists(this.cacheFilePath)) {
				loadTranslations(this.cacheFilePath);
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed resolving cache directory for session: " + session, e);
		}

		this.saveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "LLMAutoTranslate-SaveThread");
			t.setDaemon(true);
			return t;
		});
		this.saveScheduler.scheduleAtFixedRate(this::checkAndSaveCache, 5, 5, TimeUnit.MINUTES);
	}

	//-------------------------------------------------------------------
	/**
	 * Checks if the translation cache has been modified and saves it to cacheFilePath if dirty.
	 */
	public synchronized void checkAndSaveCache() {
		if (modified && cacheFilePath != null) {
			try {
				logger.log(Level.INFO, "Auto-saving modified translation cache to {0}", cacheFilePath);
				saveTranslations(cacheFilePath);
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed auto-saving translation cache to " + cacheFilePath, e);
			}
		}
	}

	//-------------------------------------------------------------------
	public void setTargetLanguage(String targetLanguage) {
		if (this.targetLanguage != null && !this.targetLanguage.equals(targetLanguage)) {
			clearCache();
		}
		this.targetLanguage = targetLanguage;
	}

	//-------------------------------------------------------------------
	public void clearCache() {
		cache.clear();
	}

	//-------------------------------------------------------------------
	/**
	 * Loads translation cache entries from a file in Key=Value (Java Properties style) format.
	 * Format per line: DIGEST=TIMESTAMP|TRANSLATION
	 *
	 * @param path File path to read from
	 * @throws IOException If file reading fails
	 */
	public void loadTranslations(Path path) throws IOException {
		if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
			logger.log(Level.WARNING, "Cannot load translations from path: {0}", path);
			return;
		}

		List<Map.Entry<String, TranslationEntry>> loadedList = new ArrayList<>();

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
					continue;
				}

				int eqIndex = line.indexOf('=');
				if (eqIndex <= 0) {
					continue;
				}

				String key = line.substring(0, eqIndex).trim();
				String valStr = line.substring(eqIndex + 1);

				int pipeIndex = valStr.indexOf('|');
				LocalDateTime time = LocalDateTime.now();
				String translation;

				if (pipeIndex >= 0) {
					String timeStr = valStr.substring(0, pipeIndex);
					try {
						time = LocalDateTime.parse(timeStr);
					} catch (DateTimeParseException e) {
						time = LocalDateTime.now();
					}
					translation = unescapeProperty(valStr.substring(pipeIndex + 1));
				} else {
					translation = unescapeProperty(valStr);
				}

				loadedList.add(Map.entry(key, new TranslationEntry(translation, time)));
			}
		}

		// Sort loaded entries by access time ascending so LRU access order is preserved
		loadedList.sort(Comparator.comparing(e -> e.getValue().getLastAccessed()));

		synchronized (cache) {
			cache.clear();
			for (Map.Entry<String, TranslationEntry> entry : loadedList) {
				cache.put(entry.getKey(), entry.getValue());
			}
		}

		if (path != null && path.equals(cacheFilePath)) {
			modified = false;
		}

		logger.log(Level.INFO, "Loaded {0} translations from {1}", loadedList.size(), path);
	}

	//-------------------------------------------------------------------
	/**
	 * Saves current translation cache entries to a file in Key=Value (Java Properties style) format.
	 * Format per line: DIGEST=TIMESTAMP|TRANSLATION
	 *
	 * @param path File path to write to
	 * @throws IOException If file writing fails
	 */
	public void saveTranslations(Path path) throws IOException {
		if (path == null) {
			throw new IllegalArgumentException("Path cannot be null");
		}

		if (path.getParent() != null && !Files.exists(path.getParent())) {
			Files.createDirectories(path.getParent());
		}

		List<Map.Entry<String, TranslationEntry>> entries;
		synchronized (cache) {
			entries = new ArrayList<>(cache.entrySet());
		}

		// Sort chronologically by last accessed time
		entries.sort(Comparator.comparing(e -> e.getValue().getLastAccessed()));

		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("# LLMAutoTranslate Cache - Language: " + targetLanguage);
			writer.newLine();

			for (Map.Entry<String, TranslationEntry> entry : entries) {
				String key = entry.getKey();
				TranslationEntry value = entry.getValue();
				String timeStr = value.getLastAccessed().toString();
				String escapedTranslation = escapeProperty(value.getTranslation());

				writer.write(key + "=" + timeStr + "|" + escapedTranslation);
				writer.newLine();
			}
		}

		if (path != null && path.equals(cacheFilePath)) {
			modified = false;
		}

		logger.log(Level.INFO, "Saved {0} translations to {1}", entries.size(), path);
	}

	//-------------------------------------------------------------------
	private String escapeProperty(String input) {
		if (input == null) return "";
		return input.replace("\\", "\\\\")
				    .replace("\n", "\\n")
				    .replace("\r", "\\r");
	}

	//-------------------------------------------------------------------
	private String unescapeProperty(String input) {
		if (input == null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '\\' && i + 1 < input.length()) {
				char next = input.charAt(i + 1);
				if (next == 'n') {
					sb.append('\n');
					i++;
				} else if (next == 'r') {
					sb.append('\r');
					i++;
				} else if (next == '\\') {
					sb.append('\\');
					i++;
				} else {
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	//-------------------------------------------------------------------
	private String computeDigest(String text) {
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
	 * Determines if a given text line should be considered ASCII art or non-translatable decoration.
	 * Calculates the ratio of alphanumeric characters (letters and digits) to total non-whitespace characters.
	 *
	 * @param text The plain text to evaluate
	 * @return true if the text is considered ASCII art/decoration and should not be translated
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
	 * @see org.prelle.terminal.ReceiveBuffer.ReadBufferHandler#onLineReceived(org.prelle.terminal.ReceiveBuffer.ReceivedLine, java.util.List)
	 */
	@Override
	public HandlerResult onLineReceived(ReceivedLine line, List<ReceivedLine> history) {
		List<AParsedElement> finalAnsi = new ArrayList<>();

		if (line == null || line.getOriginalAnsi() == null || line.getOriginalAnsi().isEmpty()) {
			return NO_CHANGE;
		}

		String plainText = line.getOriginalAsText();

		// If line has no printable text (only whitespace or control codes), keep original ANSI and don't call LLM
		if (plainText == null || plainText.trim().isEmpty()) {
			return NO_CHANGE;
		}

		// Detect ASCII art / line graphics to prevent destroying visual drawings
		if (isAsciiArt(plainText)) {
			logger.log(Level.DEBUG, "Skipping ASCII art line: {0}", plainText);
			return NO_CHANGE;
		}

		// 1. Build a tagged representation separating printable text from non-printable ANSI fragments
		List<AParsedElement> nonPrintable = new ArrayList<>();
		StringBuilder taggedText = new StringBuilder();

		for (AParsedElement element : line.getOriginalAnsi()) {
			if (element instanceof PrintableFragment pf) {
				taggedText.append(pf.getText());
			} else {
				int index = nonPrintable.size();
				nonPrintable.add(element);
				taggedText.append("<a").append(index).append("/>");
			}
		}

		String textToTranslate = taggedText.toString();

		// 2. Check translation cache using SHA-256 digest
		String cacheKey = computeDigest(textToTranslate);
		if (cache.containsKey(cacheKey)) {
			TranslationEntry entry = cache.get(cacheKey);
			if (entry != null) {
				entry.touch();
				var list = reconstructFinalAnsi(line, entry.getTranslation(), nonPrintable);
				return new HandlerResult(false, true, list);
			}
		}

		// 3. Send tagged text to the LLM
		try {
			String langName = getLanguageDisplayName(targetLanguage);
			
			// Include brief history as context if available
			StringBuilder contextBuilder = new StringBuilder();
			if (history != null && !history.isEmpty()) {
				int start = Math.max(0, history.size() - 3);
				for (int i = start; i < history.size(); i++) {
					String prevText = history.get(i).getOriginalAsText();
					if (prevText != null && !prevText.trim().isEmpty()) {
						contextBuilder.append("History line: ").append(prevText.trim()).append("\n");
					}
				}
			}

			String systemPrompt = String.format(
				"You are a real-time translator for a MUD (Multi-User Dungeon) game terminal.\n" +
				"Translate the given text into %s.\n" +
				"CRITICAL FORMATTING INSTRUCTIONS:\n" +
				"1. The text contains formatting tags like <a0/>, <a1/>, etc. Maintain ALL tags intact in your translation and place them around the translated words corresponding to the original text.\n" +
				"2. Do NOT translate, alter, or remove any tag names or tag syntax (keep <a0/> exactly as <a0/>).\n" +
				"3. Return ONLY the translated line. Do NOT include markdown blocks, quotes, or explanations.\n" +
				"%s",
				langName,
				contextBuilder.length() > 0 ? "Recent conversation context:\n" + contextBuilder : ""
			);

			logger.log(Level.DEBUG, "Translating line: {0}", textToTranslate);
			
			String rawResponse = chatModel.chat(
				SystemMessage.from(systemPrompt),
				UserMessage.from(textToTranslate)
			).aiMessage().text();

			String translatedTaggedText = cleanLLMOutput(rawResponse);
			logger.log(Level.DEBUG, "Translated: {0}", translatedTaggedText);

			// 4. Store translation in cache with current timestamp
			cache.put(cacheKey, new TranslationEntry(translatedTaggedText));
			modified = true;

			// 5. Reconstruct final ANSI list and set on line
			var list = reconstructFinalAnsi(line, translatedTaggedText, nonPrintable);
			return new HandlerResult(false, true, list);

		} catch (Exception e) {
			logger.log(Level.WARNING, "LLM translation failed for line: " + textToTranslate, e);
		}

		return NO_CHANGE;
	}

	//-------------------------------------------------------------------
	private List<AParsedElement> reconstructFinalAnsi(ReceivedLine line, String taggedText, List<AParsedElement> nonPrintable) {
		List<AParsedElement> replacementAnsi = new ArrayList<>();
		Matcher matcher = TAG_PATTERN.matcher(taggedText);
		int lastEnd = 0;

		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();

			if (start > lastEnd) {
				String textSegment = taggedText.substring(lastEnd, start);
				if (!textSegment.isEmpty()) {
					replacementAnsi.add(new PrintableFragment(textSegment));
				}
			}

			try {
				int tagIndex = Integer.parseInt(matcher.group(1));
				if (tagIndex >= 0 && tagIndex < nonPrintable.size()) {
					replacementAnsi.add(nonPrintable.get(tagIndex));
				}
			} catch (NumberFormatException nfe) {
				logger.log(Level.WARNING, "Invalid tag index in translation: " + matcher.group(0));
			}

			lastEnd = end;
		}

		if (lastEnd < taggedText.length()) {
			String remainingText = taggedText.substring(lastEnd);
			if (!remainingText.isEmpty()) {
				replacementAnsi.add(new PrintableFragment(remainingText));
			}
		}
		return replacementAnsi;
	}

	//-------------------------------------------------------------------
	private String cleanLLMOutput(String raw) {
		if (raw == null) return "";
		String str = raw.trim();
		if (str.startsWith("```")) {
			int firstNewline = str.indexOf('\n');
			int lastBackticks = str.lastIndexOf("```");
			if (firstNewline != -1 && lastBackticks > firstNewline) {
				str = str.substring(firstNewline + 1, lastBackticks).trim();
			} else {
				str = str.replace("```", "").trim();
			}
		}
		if (str.startsWith("\"") && str.endsWith("\"") && str.length() > 1) {
			str = str.substring(1, str.length() - 1);
		}
		return str;
	}

	//-------------------------------------------------------------------
	private String getLanguageDisplayName(String code) {
		if (code == null) return "German";
		Locale locale = Locale.forLanguageTag(code);
		String display = locale.getDisplayLanguage(Locale.ENGLISH);
		return (display != null && !display.isEmpty()) ? display : code;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.terminal.ReceiveBuffer.ReadBufferHandler#onConnectionLost()
	 */
	@Override
	public void onConnectionLost() {
		checkAndSaveCache();
		if (saveScheduler != null) {
			saveScheduler.shutdown();
		}
	}

}
