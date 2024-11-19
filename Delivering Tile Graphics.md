# Delivering Tile Graphics

A special challenge for a codebase that wants to deliver "graphics", but wants to be playable with every client out there is to learn which would be the best way to present tilemaps. In theory there are three major options:

1. **Via ASCII characters**
   The most backward compatible way is to represent each tile with an ASCII (or Unicode) character and a fore- and background color. That graphic can be presented next to the room description.

2. **Via inline/terminal graphics**
   It is a not so well known (and widespread) feature of terminal emulators that they are able to display inline graphics. First attempts have been made in the 90s by DEC with SIXEL graphics, modern emulators eventually support the Kitty graphics protocol or the iTerm2 image protocol - all allow displaying graphics spanning multiple text cells/lines. The problem is that either clients don't support any of the protocols or that their terminal emulator support isn't complete enough so the server can query the TE for its capabilities.
   For other connectors like Telegram or Discord, attaching images to a message is a basic feature.

3. **Via dedicated protocols**

   This option covers all ways of transmitting a tilemap using GMCP or eventually other existing protocols. For example, the BeipMu client specifies a very plain protocol to define a tileset and send map information, which the client renders in a dedicated area. 