# MUDClient-Base

`MUDClient-Base` is the core library module of RealmRunner, managing connection state, session execution (`MUDSession`), server configuration profiles, sound playback (`SoundManager`), file storage (`DataFileManager`), real-time LLM translation (`LLMAutoTranslate`), and Text-To-Speech (`AutoTTS`).

---

## 1. Package Structure

- **`org.prelle.realmrunner.network`**: Core networking and session handling (`MUDSession`, `MUDSessionBuilder`, `SoundManager`, `DataFileManager`, `MSPHandler`, `SessionConfig`, `Config`).
- **`org.prelle.realmrunner.feature.translate`**: Real-time LLM translation handler (`LLMAutoTranslate`).
- **`org.prelle.realmrunner.feature.tts`**: Text-To-Speech framework (`AutoTTS`, `TTSEngine`, `NoOpTTSEngine`).

---

## 2. Key Architecture & Components

### `MUDSession`
* Central session coordinator managing network streams, telnet option negotiation, and terminal buffer filtering.
* Handlers (`MSPHandler`, `LLMAutoTranslate`, `AutoTTS`) require a valid `MUDSession` instance during construction.

### `LLMAutoTranslate` (`org.prelle.realmrunner.feature.translate.LLMAutoTranslate`)
* **Constructor Contract**: Requires `MUDSession session` (e.g. `new LLMAutoTranslate(session, Locale.GERMAN)`). Automatically resolves session data directory using `DataFileManager.getCurrentDataDir(session)`.
* **ANSI Tagging & Preservation**: Maps non-printable `AParsedElement` fragments (SGR colors, styles) to indexed tags (`<a0/>`, `<a1/>`, ...). Translates only printable text while retaining tag positioning, reconstructing original ANSI styling upon receiving LLM response.
* **ASCII Art Detection**: Uses `isAsciiArt(text)` with `minAlphanumericRatio` threshold (default `0.4`) to bypass translation on borders, diagrams, and ASCII drawings.
* **Cache & Aging**: Keyed by SHA-256 digest with LRU eviction (`maxCacheSize`) and `LocalDateTime` access timestamps (`TranslationEntry`).
* **5-Minute Auto-Save Timer**: Uses a daemon `ScheduledExecutorService` (`saveScheduler`) to check every 5 minutes if cache entries have been modified. If dirty, saves entries to `translation_cache_<lang>.properties` under the session directory and resets the `modified` flag.

### `AutoTTS` & `TTSEngine` (`org.prelle.realmrunner.feature.tts`)
* **Constructor Contract**: Requires `MUDSession session`. Automatically resolves audio storage directory to `DataFileManager.getCurrentDataDir(session).resolve("tts")`.
* **Capability Detection (`TTSEngine`)**:
  - `boolean canSynthesize()`: Returns whether the engine can synthesize plain text into raw audio bytes.
  - `boolean canSpeak()`: Returns whether the engine can speak text directly aloud.
  - `NoOpTTSEngine` returns `false` for both capabilities as a default fallback.
* **3-Step Execution Order**:
  1. **Cached File Playback**: Checks if a cached audio file already exists on disk (`getAudioFilePath`). If found, plays it using `SoundManager.getInstance().play(session, cmd)`.
  2. **Synthesis & Storage**: If `engine.canSynthesize()` returns `true`, calls `engine.synthesize(...)`, persists raw bytes to `storageDirectory`, updates cache, and plays the audio via `SoundManager`.
  3. **Direct Speech Output**: If `engine.canSpeak()` returns `true`, falls back to calling `engine.speak(textToSpeak, language, voice)` directly.
* **Async Threading**: Executes speech processing asynchronously on a single-thread daemon executor thread to prevent blocking `ReceiveBuffer` stream parsing.

### `SoundManager` (`org.prelle.realmrunner.network.SoundManager`)
* Abstract singleton managing audio playback (`PlayCommand`, `NowPlaying`) for MUD sessions.
