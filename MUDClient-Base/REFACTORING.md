# RealmRunner Architecture Refactoring: Push-Based Pipeline & Pluggable Protocol Stack

## Objectives
Refactor RealmRunner's connection, protocol decoding, extension routing, and rendering architecture to achieve maximum flexibility across transport layers (TCP Sockets, WebSockets), protocols (Telnet with GMCP/MSP/MXP, Raw Stream, MUDDown), and rendering backends (VT100 ANSI Terminal, MUDDown Rich UI, `MUDStandards.Frame` split-pane layout manager).

For full developer architecture documentation and Mermaid diagrams, see [`ARCHITECTURE.md`](file:///home/prelle/git/RealmRunner/MUDClient-Base/ARCHITECTURE.md).

---

## Architectural Principles
1. **Push-Based Data Flow**: Replace blocking `InputStream.read()` chains with a push-based data pipeline (`onBinaryFrame(byte[])`, `onTextFrame(String)`).
2. **Stateful FSM Decoders**: Implement stateful finite state machines (`TelnetStateDecoder`, `ANSIStateParser`) to handle partial TCP packet delivery and fragmented subnegotiations cleanly across chunk boundaries.
3. **Decoupled Event Bus (`MUDEventBus`)**: Out-of-band features (GMCP, Sound/MSP, Frame Layouts, Stream Lines) publish typed `MUDEvent`s to a session-scoped bus rather than executing side-effects inside stream parsers.
4. **Standalone Library Agnosticism & Bridge Adapters**: 3rd party libraries (`TelnetLibrary`, `GMCP4J`, `libmxp`) remain 100% event-bus agnostic. Bridge adapters (`GMCPEventBridge`, `MXPEventBridge`, `TelnetOptionManager`) bridge library callbacks to `MUDEventBus`.
5. **Pluggable Renderers**: Abstract presentation behind `RenderEngine` to seamlessly support VT100 ANSI terminals (JFX, GhosttyFX, JediTermFX, VT100 CLI) and structured MUDDown / Split-Frame UI views.

---

## Refactoring ToDo List

### Phase 1: Event Bus & Extension Architecture
- [x] **Define `MUDEventBus` and Domain Event Hierarchy** (`org.prelle.realmrunner.event`)
  - [x] `MUDEvent` (base abstract class with session & timestamp)
  - [x] `MUDEventBus` (thread-safe, session-scoped typed event bus)
  - [x] `GMCPEvent` (strictly carries strongly-typed `GMCPCommand` POJO)
  - [x] `SoundEvent` (integrated sound properties directly into event)
  - [x] `FrameControlEvent` (`MUDStandards.Frame` layout commands: open, close, update, stream)
  - [x] `StreamLineEvent` (printable ANSI elements & plain text line)
- [x] **Wire Extension Handlers to Event Bus**
  - [x] Expose `MUDEventBus` on `MUDSession` via `@Getter getEventBus()`.
  - [x] Subscribe `SoundManager` directly to `SoundEvent` on `MUDEventBus` (`registerSession(MUDSession)`).
  - [x] Automatically publish `GMCPEvent` in `MUDSession.setupGMCP()`.

### Phase 2: Stateful Protocol Decoders & Event Bridges
- [x] **Build Stateful `TelnetStateDecoder` Adapter** (`org.prelle.realmrunner.decoder`)
  - [x] Delegate push byte-slicing and partial packet buffering to `TelnetStateMachine` from `TelnetLibrary`.
  - [x] Delegate option negotiation and subnegotiations directly to `TelnetProtocol` and registered `TelnetOption` handlers (`GMCPHandler`, `MUDSoundProtocolOption`, `MXPOption`).
  - [x] Forward clean display bytes downstream to `ANSIStateParser`.
- [x] **Build Stateful `ANSIStateParser` (DEC VT500 FSM)** (`org.prelle.realmrunner.decoder`)
  - [x] Accept clean display bytes from `TelnetStateDecoder`.
  - [x] Delegate VT500 sequence parsing state tracking to `VT500Parser` from `libansi`.
  - [x] Emit parsed `AParsedElement` fragments (`PrintableFragment`, `SelectGraphicRendition`, `C0Fragment`, `C1Fragment`, `ControlSequenceFragment`, `EscapeSequenceFragment`, `DeviceControlFragment`, `StringMessageFragment`).
- [x] **Build `MUDDownDecoder`** (`org.prelle.realmrunner.decoder`)
  - [x] Parse text/JSON frames for WebSockets using the `muddown` subprotocol.
- [x] **Build Event Bridge Adapters & Option Manager** (`org.prelle.realmrunner.network.bridge`)
  - [x] `GMCPEventBridge`: Translates `GMCPReceiver` callbacks from `GMCP4J` to `GMCPEvent` published on `MUDEventBus`.
  - [x] `MXPEventBridge`: Translates `MXPListener` callbacks from `libmxp` to session events.
  - [x] `TelnetOptionManager`: Configurable option coordinator allowing selective enabling/disabling of Telnet options (EchoMode, NAWS, TTYPE, GMCP, MSP, MXP) per session or test scenario.

### Phase 3: Transport Layer Abstraction
- [x] **Define `MUDTransport` and `MUDTransportListener` Interfaces**
  - [x] Push callbacks: `onBinaryFrame(byte[])`, `onTextFrame(String)`, `onConnected(subprotocol)`, `onDisconnected()`.
- [x] **Implement `TCPTransport`**
  - [x] Dedicated daemon reader thread reads available socket bytes into a fixed buffer (4KB chunk) and immediately invokes `listener.onBinaryFrame(chunk)`.
- [x] **Implement `WebSocketTransport`**
  - [x] Wraps `jakarta.websocket.Session`.
  - [x] Inspects `session.getNegotiatedSubprotocol()`.
  - [x] Routes `@OnMessage` binary/text frames directly to `listener`.

### Phase 4: Pipeline Assembly & `MUDSession` Refactoring
- [x] **Create `SessionPipelineFactory`**
  - [x] Assemble push chain based on transport and negotiated subprotocol (`telnet.mudstandards.org`, `muddown`, `terminal.mudstandards.org`):
    - `Transport` $\rightarrow$ `ProtocolDecoder` $\rightarrow$ `ANSIStateParser` $\rightarrow$ `StreamLineEvent` / `MUDEventBus`.
- [x] **Refactor `MUDSessionBuilder` & `MUDSession` Pipeline Capabilities**
  - [x] Defaults to `TCPTransport` (or `WebSocketTransport` if `clientConfig.getProtocol() == SessionProtocol.WEBSOCKET`).
  - [x] Added `setTransport(MUDTransport)` to allow explicitly overriding or mocking transport implementations.
  - [x] Integrated `SessionPipelineFactory` for push-based transport dispatching and option management.

### Phase 5: Presentation & Render Engine Decoupling
- [x] **Define `RenderEngine` Interface**
  - [x] Standardize output rendering methods (`renderLine`, `handleFrameControl`, `clear`).
- [x] **Adapt Terminal Renderers (`AnsiTerminalRenderEngine`)**
  - [x] Wrap terminal visualizers (`libterminal-jfx`, `GhosttyFX`, `JediTermFX`, VT100 CLI) and subscribe to `StreamLineEvent` and `FrameControlEvent`.
- [x] **Implement Split-Frame Layout Controller (`MUDStandards.Frame`)**
  - [x] Listen to `FrameControlEvent`s on `MUDEventBus` for dynamic pane splitting.

### Phase 6: Verification & Dogfooding
- [ ] **Test TCP Telnet MUDs**: Verify ANSI colors, GMCP, MSP, LLM translation, and TTS work cleanly over raw TCP with partial packet arrival simulation.
- [ ] **Test WebSocket MUDs**: Verify subprotocol negotiation (`telnet.mudstandards.org`, `muddown`, `terminal.mudstandards.org`) over WebSocket server connections.
