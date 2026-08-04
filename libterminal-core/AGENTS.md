# AGENTS.md

This document serves as context, architectural guidance, and operational knowledge for AI coding agents working on `libterminal-core`.

---

## 1. Project Overview

`libterminal-core` (artifactId: `libterminal-api`) is a Java 21+ library providing base interfaces, stream wrappers, and input/output multiplexing logic for terminal emulator integration (e.g. `libterminal-ghosttyfx`, `libterminal-jeditermfx`).

* **Package**: `org.prelle.terminal`
* **Target Java Version**: Java 21+
* **Build System**: Maven (`pom.xml`)

---

## 2. Architecture & Design Patterns

### `SwitchableInputStream` (`org.prelle.terminal.SwitchableInputStream`)
Multiplexes a primary `InputStream` source (e.g. `NewANSIInputStream` / network socket) with locally injected data bytes (`injectedData`).

* **Locking & Thread Safety Rule**:
  * Microsecond locking (`synchronized (lock)`) is used exclusively to append to or remove from the `injectedData` list and call `lock.notifyAll()`.
  * **CRITICAL**: Network reads (`source.read(...)`) MUST NEVER be executed inside a `synchronized` lock block. Executing blocking reads while holding the monitor lock causes deadlocks on UI application threads (such as the JavaFX Application Thread) when user input is injected.
* **Signed Byte Masking**:
  * Byte values appended to `injectedData` must be masked (`b & 0xFF`) to prevent negative signed byte values (`0x80..0xFF`) from being treated as stream EOF (`-1`).

### `EchoChamber` (`org.prelle.terminal.EchoChamber`)
Filter stream wrapping the output stream (`OutputStream`) and forwarding typed user input into `SwitchableInputStream` when `echoEnabled == true`.

* Controlled via `setEchoEnabled(boolean)`.
* When `echoEnabled == false` (e.g. password prompts or Telnet `IAC WILL ECHO`), user input is sent to the server output stream but suppressed from local injection.

### `SwitchableOutputStream` (`org.prelle.terminal.SwitchableOutputStream`)
Wraps output blocks and delegates to a dynamic `sink` (`EchoChamber`). Overrides block write methods (`write(byte[], off, len)` and `write(byte[])`) to forward arrays directly to the sink without byte-by-byte iteration overhead.

---

## 3. ReceiveBuffer & LLM Translation Stack

### `ReceiveBuffer` & `ReadBufferHandler` (`org.prelle.terminal.ReceiveBuffer`)
Decodes incoming ANSI stream fragments (`AParsedElement`) and passes full lines (`ReceivedLine`) to registered `ReadBufferHandler` instances before outputting to the terminal.

* **`ReceivedLine`**: Contains `originalAnsi` (raw incoming elements), `originalAsText` (plain text), `raw` (`byte[]` array of concatenated raw fragment bytes via `AParsedElement.getRaw()`), and `finalAnsi` (elements sent to the terminal).
* **Handler Contract**: Handlers implement `boolean onLineReceived(ReceivedLine line, List<ReceivedLine> history)`. Returning `false` allows line processing to continue. If `finalAnsi` is empty when unconsumed, `ReceiveBuffer` populates `finalAnsi` with `originalAnsi`.

### `LLMAutoTranslate` (`org.prelle.terminal.LLMAutoTranslate`)
Translates MUD line text in real time using LangChain4j `ChatModel` (e.g., Ollama `qwen3:8b`).

* **ANSI Preservation**: Replaces non-printable `AParsedElement` fragments (colors, SGR styling) with index tags (`<a0/>`, `<a1/>`, ...). Translates only printable text while preserving tags in position, then reconstructs `finalAnsi` with original styling objects intact.
* **ASCII Art & Decoration Filter**: `isAsciiArt(String text)` calculates the ratio of alphanumeric characters (`Character.isLetterOrDigit`) to non-whitespace characters. Lines below `minAlphanumericRatio` (default `0.4`) are identified as ASCII art/borders and bypass LLM translation to avoid corrupting terminal visuals.
* **SHA-256 Digest Caching**: Computes SHA-256 hashes of tagged strings to serve as compact cache keys, reducing memory overhead.
* **LRU Memory Retention**: Backed by a synchronized access-order `LinkedHashMap` bounded by `maxCacheSize` (default: 5,000 entries) to evict oldest translations and prevent memory leaks.

---

## 4. Terminal Emulator Lifecycle Contracts

1. **`setLocalEchoActive(boolean)`**:
   * Telnet option negotiation (`IAC WILL/WONT ECHO`) calls `setLocalEchoActive` during connection setup.
   * Can be invoked **before** `connectWith(in, out)` instantiates `echoChamber`.
   * Implementations (`TerminalEmulator`) MUST preserve `localEcho` state in an instance field and initialize `echoChamber.setEchoEnabled(this.localEcho)` inside `connectWith(...)`.

2. **User Input Routing (`sendUserInput`)**:
   * UI components (e.g. input textfields) must route text through `sendUserInput(String text)`.
   * `sendUserInput` must write `(text + "\r\n")` directly to `outPipe` (which routes to `EchoChamber`), ensuring input is sent across the socket and conditionally echoed based on `localEcho`.

---

## 5. Build and Test Commands

* **Compile codebase**:
  ```bash
  mvn clean compile
  ```
* **Run tests**:
  ```bash
  mvn test
  ```
* **Install to local repository**:
  ```bash
  mvn clean install -DskipTests
  ```
