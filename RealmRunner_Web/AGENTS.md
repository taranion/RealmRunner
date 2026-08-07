# RealmRunner Web

This project is a MUD client. It does connect to the MUD using Websockets over which Telnet is tunnelled. The primary feature of this client is that he will be able to split off docked webviews that shows URLs dynamically provided by the server and send or receive GMCP messages to or from them.

## Technology

* ghostty-web
  A drop-in replacement for xterm.js (Source: https://github.com/coder/ghostty-web) will process ANSI sequences
* Websocket
  RFC 6455, using subprotocols defined here: https://mudstandards.org/websocket/
  The subprotocol to implement is "telnet.mudstandards.org"
* Telnet Options
  NAWS, LINEMODE, ECHO, CHARSET, TTYPE, EOR, SGA
* MUD specific telnet options
  GMCP for out-of-band message transfer and music, MNES, MTTS, MXP
* Relevant GMCP packages (see here: https://mudstandards.org/gmcp/)
  * "client.media" for Sound/Music
  * "mudstandards.tilemap" for a graphical tilemap
  * "webview"

## Rough architecture

A single page web page is either configured with parameter for host + port as URL parameters or opens a connection dialog. Once enough data is known, a websocket connection is openend. All traffic usually sent over a normal telnet connection will be transferred here.

There must be a full telnet stack handling this data, to enable working with telnet options correctly. The client will handle some protocol commands by itself - e.g. "client.media.play" to play audio files. Others will be handled by extensions

## WebViews

A central feature is processing the "webview.open" or "webview.close" GMCP command (see https://mudstandards.org/gmcp/webview/ or here with more details: https://github.com/BeipDev/BeipMU/blob/master/Documentation/WebViews.md#gmcp). With those the MUD server orchestrates which web pages should be displayed next to or above the main window. 

A webview should be able to register hooks or call methods to subscribe or send GMCP messages. The original standard declares `window.chrome.webview.hostObjects.client` as an object for that - we need to decide what to use so that it works in all browsers.

