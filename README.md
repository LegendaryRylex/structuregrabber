# Structure Grabber

Grab any region of the world into a structure `.snbt` file, the way `/fill` takes a region to fill it.

Client side only, so the server never needs the mod.

```
/structuregrabber <from> <to> [name]
```

Both corners take the coordinates `/fill` takes, absolute or `~` relative, and the region between them is written to
`structuregrabber\<name>.snbt` in the game directory, named after the time when no name is given. Chat links the
finished file. Corners can also be set one at a time while you walk a build, with `pos1`, `pos2`, `grab` and `clear`.

The result is a plain structure template: drop it into a datapack under `data\<namespace>\structure\`, or load it in a
structure block.

The in-game config sets the output folder, whether entities and air are saved, and how large a region may be.

In single player the grab reads the world the game is running, so block entities are complete and chunks load on
demand. On a server it can only use what the client was sent, which leaves out chest and machine contents, and it
refuses regions whose chunks are not loaded.

## Building

```
.\gradlew build
```

Jar lands in `build\libs\`.

## Development

- `.\gradlew runClient` / `runServer` / `runData` / `runGameTestServer`
- `.\gradlew spotlessApply` before committing
