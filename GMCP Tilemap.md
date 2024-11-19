[TOC]

# GMCP Tilemap

The purpose of this package is to allow MU\* servers to control coordinate based maps. It is inspired from the `beip.tilemap` ([link](https://github.com/BeipDev/BeipMU/blob/master/TileMap.md)) package, but adds features to work with multiple tilesets per map, layering and options to choose tileset resolutions.

## tilemap.tilesets

With this command the server informs the client about all tilesets available and assigns identifiers to reference them in later commands. Tilesets may come in different resolutions - the client is free to pick the one suited best.

```json
tilemap.tilesets {
    "terrain": {
        "size16": {
            "type": "PLAIN",
            "size": "16x16",
			"url": "http://.../SmallTiles.png"
        },
        "size32": {
            "type": "PLAIN",
            "size": "32x32",
            "url" : "http://.../LargeTiles.png"
        }
    },
	"monster": {
        "size16": {
            "type": "SPRITESHEET",
            "size": "16x16",
            "url": "https://...",
            "anim-frames": {
                "0": "4",
                "15": "4"
            }
        }
    }        
  }
}
```

- **type**  - one of [PLAIN|SPRITESHEET]
  In a PLAIN tileset, there are no animation frames. Each tile gets a number corresponding its position in the image.
  In a SPRITESHEET a consecutive set of tiles may define a single sprite. Those sprite is referenced by the first number

- **anim-frames** - Those tiles that are animated, are referenced by their number and have the number of animation frames assigned.

  > [!IMPORTANT]
  >
  > Tiles not listed in anim-frames are considered to consist of only 1 frame, meaning that they are not animated.
  >
  > A client not supporting animation can just ignore animation frames.

  

## tilemap.area

This command tells the client to prepare one or more areas that should show tilemaps. Each area can make use of all tilesets that have been uploaded by `tilemap.tilesets` before. If the tilesets are provided in multiple resolutions, it is up to the client to pick the one used - e.g. because the user chooses a resolution or because of the screen size.



```json
tilemap.area {
    "World Map": {
        "map-size": "21x21",
        "encoding": "Hex_8",
        "tilesets": {
            "terrain": "0",
            "monster": "128"
        }
    },
    "Surrounding": {
        "map-size": "11x11",
        "encoding": "Hex_16",
        "tilesets": {
            "terrain": "0",
            "objects": "128",
            "monster": "256"
        }
    }
}
```

## tilemap.data

This command will be send for each update in the map.

```json
tilemap.data {
	"Surrounding": {
		"0": "<string of layer data, depending on encoding",
		"1": "<string of layer data, depending on encoding"
	}
}
```

------

[Beip.Tilemap]: https://github.com/BeipDev/BeipMU/blob/master/TileMap.md	"Beip.Tilemap Package"

