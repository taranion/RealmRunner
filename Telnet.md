# The Telnet Connector

Since their early days MU\*s have been accessed via a Telnet connection. In the simplest case a normal telnet client can be used to access a MUD, but usually dedicated client applications are used, since they provide added features.

Two elements define the technical possibilities for the user experience:

- **The feature set of the terminal emulator**
  This usually includes text styles and colors, but also the ability to show graphics, show sophisticated layout in the terminal screen or the ability to transmit typed keys rather than complete input lines
- **The feature set of the client application**
  This ranges from automatically reacting to output the server sends (Trigger), over playing sounds and music up to special extensions to have more complex user interfaces.

There are a lot of different feature details and no client supports all of them - in case of clients or telnet running in a terminal emulator, the terminal emulator used has a strong influence.

------

## Detection phases

The *Telnet Connector* will try to detect the capabilities of the connecting client by supporting several *Telnet options* as well as sending commands to the terminal emulator and learn from the reactions or the lack of.

### Telnet handshakes

Directly after there was an incoming connection, the server starts sending several Telnet options:

- **[LINEMODE](https://datatracker.ietf.org/doc/html/rfc1184)** (RFC 1184)
  Clients with support for the EDIT flag, can be configured to send keystrokes instead of input lines, which is required for step movement on maps.
- **[CHARSET](https://www.iana.org/go/rfc2066)** (RFC 20266)
  As the name implies, this option allows client and server to select a charset they both support.
- **[TERMINAL-TYPE](https://www.iana.org/go/rfc1091)** (RFC 1091) or **[MTTS](https://tintin.mudhalla.net/protocols/mtts/)**
  The regular TERMTYPE option is restricted to return the identifier of terminals the emulator is compatible to - which is nowadays not a good indicator for capabilities anymore.
  Instead MUD clients usually use the **M**UD **T**erminal **T**ype **S**tandard which is more expressive with regard to individual features - though not sufficient.
- **[NAWS](https://datatracker.ietf.org/doc/html/rfc1073)** (RFC 1073)
  This allows the server to always know the size of the terminal window of the client
- **[NEW ENVIRONMENT OPTION](https://www.iana.org/go/rfc1572)** (RFC 1572) or [MNES](https://tintin.mudhalla.net/protocols/mnes/)
  This option allows the client to transmit variables, which in case of the RFC is of no real use, while the MU\* specific MNES standard transmits information like a client name, the MTTS information or other information a client thinks useful.
- **[MSP](https://www.zuggsoft.com/zmud/msp.htm)** - MUD Sound Protocol
- **[GMCP](https://tintin.mudhalla.net/protocols/gmcp)** - Generic MUD Client Protocol
  This is not a specific protocol, but a way to transmit any kind of information specified in extensions. Some of those extension packages are widespread, others specific to certain servers or clients.
- **MSDP** - MUD Server Data Protocol

### Terminal Handshake

After the Telnet handshake is done, the Telnet connector tries sending several command sequences to the clients terminal. Those sequences should trigger a response and by the response or the lack of, the connector gets knowledge of supported terminal features.

- **Send XTGETTCAP for "TN"**

  This Xterm specific command has been adopted by several terminal emulators and returns with the terminal name. This may or may not be identical to the TERMINAL TYPE option above.

- **Send XTGETTCAP for "Co"**
  This command returns the number of colors that can be addressed in the terminal

- **Send a zero-sized image via the [iTerm2 graphics protocol](https://iterm2.com/documentation-images.html)**
  Any kind of matching reaction tells the server that the protocol is understood.

- **Send a zero-sized image via the [Kitty graphics protocol](https://sw.kovidgoyal.net/kitty/graphics-protocol/)**
  Same as above - if a matching response is received the protocol is assumed to be supported

- **Query for primary device attribute (DA)**
  The response to this ANSI command returns the mode the terminal is operating (e.g. VT220) and a supported feature set (like Sixel graphics)

- **Query for secondary device attributes (DA2)**
  Like above, but returns the hardware device model the terminal emulator pretends to be

- **Query and modify cursor positon**
  The server queries the current cursor position, sends a command to move the cursor and queries again to check if that was successful. Afterwards the cursor position is restored.

Experience shows that while pure telnet clients do return terminal responses, all dedicated MUD clients don't - usually because of their reduced terminal emulation or because they filter terminal responses.