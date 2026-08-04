# Projekt "Realm Runner"

This project is a MUD client, a software to enhance the experience of playing Multi-User-Dungeons (MUDs).
It comes in three "flavors": 
- a version to be run in a VT100 compatible terminal emulator (RealmRunner_CLI)
- a version with a graphical user interface (RealRunner_JFX)
- a version with a webfrontend (RealmRunner_Web)
All three versions share a common backend (MUDClient-Base, libterminal-*)

Key features compared to standard MUD clients include out-of-the-box support for GMCP extensions, mapping, and real-time LLM-assisted line translation with ANSI styling preservation.

## Technology
The used programming language is Java (Java 23 and newer). Supported operating systems are Windows, Linux and OS X,
while mobile versions are expected to use a Webfrontend provided by RealmRunner_Web
For the graphical version, the JavaFX UI framework is used.
Project compilation is done using Maven.

The dependency on external libraries should be reduced to a minimum. Exceptions are all dependencies
from the GraphicMUD project and LangChain4j for LLM translation.

## Architecture
- **MUDClient-Base**: Connection handling, session management (`MUDSession`), storing MUD server profiles, translation, and TTS.
  - **`LLMAutoTranslate`** (`org.prelle.realmrunner.feature.translate`): Real-time translation handler powered by LangChain4j. Features:
    - ANSI styling preservation by mapping non-printable `AParsedElement` fragments to index tags (`<a0/>`, `<a1/>`, ...).
    - ASCII art detection via alphanumeric-to-character ratio checking (`isAsciiArt`) to prevent corrupting visual diagrams/borders.
    - SHA-256 digest hashing for compact cache keys.
    - LRU eviction policy with bounded memory cache (`maxCacheSize`).
    - 5-minute scheduled auto-save timer writing modified cache entries to disk.
  - **`AutoTTS`** (`org.prelle.realmrunner.feature.tts`): Real-time Text-To-Speech handler. Features:
    - Pluggable `TTSEngine` interface with capability detection (`canSynthesize()`, `canSpeak()`).
    - 3-step playback strategy using `SoundManager` for cached/synthesized files and direct speech fallback.
    - ASCII art detection to prevent synthesizing drawing / border noise.
    - Individual audio file disk persistence (`<digest>.wav`) and LRU caching (`TTSEntry` with `LocalDateTime` timestamps).
- **libterminal-core / libterminal-api**: Abstract API to the terminal emulation layer, input multiplexing (`SwitchableInputStream`), and stream buffer filtering (`ReceiveBuffer`, `ReadBufferHandler`).
- **libterminal-native**: API implementation for a native VT100 compatible terminal emulator.
- **libterminal-emulated**: Terminal emulator managing screens and cells without visual coupling.
- **libterminal-jfx**: JavaFX visualization building on `libterminal-emulated`.
- **libterminal-jeditermfx**: Alternative visualization based on JediTermFX.
- **RealmRunner-Web**: Vaadin web server providing terminal emulation via `xterm.js`.
