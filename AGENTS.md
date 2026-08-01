# Projekt "Realm Runner"

This project is a MUD client, a software to enhance the experience of playing Multi-User-Dungeons (MUDs).
It comes in three "flavors": 
- a version to be run in a VT100 compatible terminal emulator (RealmRunner_CLI)
- a version with a graphical user interface (RealRunner_JFX)
- a version with a webfrontend (RealmRunner_Web)
All three versions share a common backend (MUDClient-Base, libterminal-*)

A key feature of this client compared to user MUD clients is the out-of-the-box support for GMCP extensions and mapping.

## Technology
The used programming language is Java (Java 23 and newer). Supported operating systems are Windows, Linux and OS X,
while mobile versions are expected to use a Webfrontend provided by RealmRunner_Web
For the graphical version, the JavaFX UI framework is used.
Project compilation is done using Maven.

The dependency on external libraries should be reduced to a minimum. Exceptions are all depencies
from the GraphicMUD project.

## Architecture
- MUDClient-Base does connection handling and storing MUD server the user wants to remember.
- libterminal-core/libterminl-api provides an abstract API to the terminal emulation layer
- libterminal-native implements the API for a native VT100 compatible terminal emulator
- libterminal-emulated is an attempt to write an terminal emulator (managing screens and cells)
  without dealing with means of visualizing it
- libterminal-jfx builds on libterminal-emulated and adds a visualization based on JavaFX
- libterminal-jeditermfx is an alternative approach for (-emulated plus -literminal-jfx), based on JediTermFX
- RealmRunner-Web provides a Webserver using Vaadin. The terminal emulation is done using a page that loads "xterm.js" and interacts with it 

