  To design a truly flexible, pluggable architecture that supports multiple transport types (TCP, WebSockets), diverse protocol stacks
  (Telnet with GMCP/MSP vs. Raw MUDDown text/JSON), and interchangeable rendering engines (ANSI terminal vs. MUDDown rich UI), we can
  adopt a 4-Layer Event-Driven Pipeline Architecture.

  This decouples Transport → Protocol / Parsing → Event Routing → Rendering.
  ──────
  ### High-Level Architecture Overview
    flowchart TD
        subgraph Transport ["1. Transport Layer (MUDTransport)"]
            TCP["TCP Transport"]
            WS["WebSocket Transport (Subprotocol: telnet, muddown, terminal)"]
        end
    
        subgraph Protocol ["2. Protocol / Pipeline Layer"]
            Negotiator["Payload / Pipeline Negotiator"]
            TelnetStack["Telnet Protocol Stack (IAC, GMCP, MSP, MXP)"]
            MUDDownDecoder["MUDDown Frame / Payload Decoder"]
            RawDecoder["Raw Text Stream Decoder"]
        end
    
        subgraph EventBus ["3. Event & Extension Router (MUDEventBus)"]
            GMCPRouter["GMCP / Event Router"]
            LayoutEvents["MUDStandards.Frame (Layout / Split Screen Events)"]
            AudioEvents["MSP / Sound Events (SoundManager)"]
            StreamEvents["Line Received Events (LLMAutoTranslate, AutoTTS)"]
        end
    
        subgraph Render ["4. Presentation / Rendering Engines (RenderEngine)"]
            VT100Engine["VT100 ANSI Engine (libterminal-emulated, JFX, GhosttyFX, CLI)"]
            MUDDownEngine["MUDDown Rich UI Engine (Markdown / Dynamic Panels)"]
            LayoutEngine["Docking / Split-Frame Layout Controller"]
        end
    
        Transport --> Negotiator
        Negotiator -- "telnet.mudstandards.org" --> TelnetStack
        Negotiator -- "muddown" --> MUDDownDecoder
        Negotiator -- "terminal.mudstandards.org" --> RawDecoder
    
        TelnetStack -- Out-of-band GMCP / MSP --> GMCPRouter
        TelnetStack -- Terminal Byte Stream --> StreamEvents
        MUDDownDecoder -- UI / Document Structure --> LayoutEvents
    
        GMCPRouter --> LayoutEvents
        GMCPRouter --> AudioEvents
        StreamEvents --> VT100Engine
    
        LayoutEvents --> LayoutEngine
        AudioEvents --> SoundManager
        VT100Engine --> LayoutEngine
        MUDDownEngine --> LayoutEngine
    ──────
  ### Layer-by-Layer Breakdown

  #### 1. Transport Layer (MUDTransport)

  Decouples how raw frames/bytes are read and written from the network.

  • Interface: MUDTransport emits raw data events (onBinaryFrame(byte[]), onTextFrame(String), onConnected(), onDisconnected()).
  • Implementations:
      • TCPTransport: Uses standard socket streams.
      • WebSocketTransport: Wraps jakarta.websocket.Session and inspects session.getNegotiatedSubprotocol() upon handshake.


    public interface MUDTransport {
        void connect(URI uri) throws Exception;
        void sendBinary(byte[] data);
        void sendText(String text);
        void setListener(MUDTransportListener listener);
        String getNegotiatedSubprotocol(); // e.g. "telnet.mudstandards.org", "muddown", etc.
        void close();
    }
    ──────
  #### 2. Protocol / Pipeline Layer (PayloadDecoder)

  The WebSocket draft delays payload interpretation until the handshake resolves or subprotocol is negotiated. A PayloadDecoder takes raw
  transport output and parses protocol-specific structures.

  • Telnet Payload Handler (TelnetPayloadDecoder):
      • Runs Telnet state machine (IAC WILL/WONT/DO/DONT, SB ... SE).
      • Strips Telnet commands and passes clean display bytes to the terminal buffer.
      • Emits out-of-band payloads (e.g. GMCPPackage, MSPSoundCommand).
  • MUDDown Payload Handler (MUDDownPayloadDecoder):
      • Parses MUDDown markdown blocks, inline metadata, and structured JSON frames.
  • Raw Payload Handler (RawStreamDecoder):
      • Passes text/ANSI directly without Telnet stripping.


    public interface PayloadDecoder {
        /** Processes incoming raw bytes or text from the transport layer */
        void processIncomingData(byte[] binaryData, String textData);
        
        /** Binds event sinks for parsed out-of-band events and presentation streams */
        void setEventPublisher(MUDEventPublisher publisher);
    }
    ──────
  #### 3. Event & Extension Router (MUDEventBus)

  Instead of coupling GMCP or sound directly to a single terminal view, out-of-band data is published as domain events on a session-
  scoped MUDEventBus.

  • GMCPEvent: Client.GUI, MUDStandards.Frame, Comm.Channel.
  • SoundEvent: Triggers SoundManager playback regardless of whether sound came from MSP (!!SOUND) or GMCP (Sound.Play).
  • StreamLineEvent: Triggers LLMAutoTranslate and AutoTTS pipelines on printable lines.

    public class MUDEventBus {
        public void publish(MUDEvent event) { ... }
        public <T extends MUDEvent> void subscribe(Class<T> eventType, Consumer<T> listener) { ... }
    }
    ──────
  #### 4. Presentation & Rendering Layer (RenderEngine & Layout Manager)

  To support both ANSI VT100 emulation and MUDDown (or hybrid frame layouts like MUDStandards.Frame), rendering is abstracted behind a
  RenderEngine.

  • AnsiTerminalRenderEngine:
      • Uses ReceiveBuffer and AParsedElement (with LLMAutoTranslate tag reconstruction).
      • Renders to VT100 views (JavaFX libterminal-jfx, GhosttyFX, or VT100 CLI).
  • MUDDownRenderEngine:
      • Renders Markdown documents, collapsible sections, styled text, and interactive buttons.
  • SplitFrameLayoutEngine (MUDStandards.Frame):
      • Listens to frame creation events (e.g., Frame.Open, Frame.Stream) and creates/manages split panes (Map view, Inventory sidebar,
      Combat log, Main prompt).


    public interface RenderEngine {
        /** Render incoming line or structured block */
        void render(RenderableContent content);
        
        /** Reset or resize rendering view */
        void clear();
    }
    ──────
  ### How Dynamic Negotiation Works in Practice

  When a connection opens:

  1. Connect: WebSocketTransport connects to ws://mud.example.com.
  2. Inspect Subprotocol:
      • If subprotocol is "telnet.mudstandards.org" (or plain TCP socket):
      • Instantiate TelnetPayloadDecoder.
      • Register AnsiTerminalRenderEngine.
      • Attach GMCPHandler, LLMAutoTranslate, and AutoTTS to the event bus.
      • If subprotocol is "muddown":
      • Instantiate MUDDownPayloadDecoder.
      • Register MUDDownRenderEngine.
      • Layout Manager prepares structured document view.

  3. Mid-Stream Negotiation / Fallback:
      • If the WebSocket server switches payload mode mid-session via a control frame, the PayloadDecoder swapper simply unbinds the old
      decoder and attaches the new one to the existing MUDEventBus.

  ──────
  ### Advantages of this Refactoring

  1. Complete Decoupling: Transports don't know about ANSI or Telnet; renderers don't know about WebSockets or TCP.
  2. Reusability: LLMAutoTranslate and AutoTTS operate on abstract line events without caring if the source was Telnet, MUDDown, or
  WebSocket.
  3. Multi-Platform Support: CLI, JavaFX, and Web (Vaadin/xterm.js) can share 100% of the Transport, Protocol, and Event layers,
  replacing only the final RenderEngine and Layout Controller.
