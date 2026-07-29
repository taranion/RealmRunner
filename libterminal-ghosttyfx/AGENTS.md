This library is part of a bigger project to provide a MUD client. The MUD client requires a terminal emulator (TE) component. To make the emulator component easily exchangeable, one of that projects foundation libraries, "LibTerminal API" (see ../libterminal-core/), defines an Java Interface `TerminalEmulator` every TE component must implement.

This specific library encapsulates *GhosttyFX* (see https://github.com/vlaaad/ghosttyfx - a small wrapper for the Ghostty terminal emulators native core), which provides Ghostty as a JavaFX component.

`io.github.vlaaad.ghosttyfx.TerminalView` is this component and it expects a TerminalFactory that returns a class that implements  `io.github.vlaaad.ghosttyfx.Terminal` - the interface of the TerminalView to send and receive data.

This wrapper project has `org.prelle.jeditermfxterminal.GhosttyTerminalView` as the central component, that implements `Terminal` as well as `TerminalEmulator`.

There are several challenges:

* The `GhosttyTerminalView` must be initialized before a connection to a MUD exists. The `SwitchableInputStream` and `-OutputStream` take care of that by providing Streams that can connect later.
* The `TerminalView` component only uses `read(byte[])` when reading input from the server.
* Confusing: The `Terminal` interface defines methods like `output()` and `input()` that returns exactly the other kind of stream, because of the different perspective.
* The MUD often needs to control the echo behavior of the terminal - but the `TerminalView` doesn't support that natively. This must either be solved in Java code ... or maybe there are ANSI sequences that control Ghosttys echo behavior.
  The `TerminalEmulator` interface defines a `setLocalEcho` method for that.
* The MUD may want to switch between a LINEMODE (typed input is just transmitted if the user hit ENTER) or RAW (or character-a-time) mode. The `TerminalEmulator` interfaces defines a `setMode` method for that.
* The client must be able to inject data into the streams - in the stream that gets displayed on the terminal, as well as on the stream that sends data to the server.