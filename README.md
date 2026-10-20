# WebDisplays (1.20.1)

Browse the web on huge in-game screens. Ported to **1.20.1** for **Fabric**, **Forge** and **NeoForge**
from the original 1.12.2 Forge mod.

WebDisplays renders a live Chromium browser (through [MCEF](../mcef)) onto multiblocks made of screen
blocks, and adds the peripherals, upgrades and tools that go with it.

## Status

| Loader | Build | Artifact |
|---|---|---|
| Fabric | passing | `fabric/build/libs/webdisplays-1.20.1-1.1.jar` |
| Forge 47.x | passing | `forge/build/libs/webdisplays-1.20.1-1.1.jar` |
| NeoForge 47.1.x | passing | `neoforge/build/libs/webdisplays-1.20.1-1.1.jar` |

See [PORTING-STATUS.md](PORTING-STATUS.md) for what is ported, what changed, and what is still missing.

## What you can do

* Build a wall of screen blocks (at least 2×2) and right-click it to turn it into a screen.
* Sneak-right-click a screen to change its URL.
* Use the **screen configurator** to set the URL, resolution, rotation, auto volume and the friend list.
* Insert **upgrades** by right-clicking a screen while holding one:
  laser sensor, redstone input, redstone output, GPS.
* Link a peripheral to a screen with the **linking tool**.
* Open a browser in your hand with the **minePad**.

## Layout

```
common/     shared sources: blocks, block entities, items, packets, client renderer and GUIs,
            plus the untouched miniserv file host
fabric/     Fabric entry point, registrar and networking
forge/      Forge entry point, registrar and networking
neoforge/   NeoForge 1.20.1 entry point (Forge packages, see PORTING-STATUS)
legacy/     the original 1.12.2 sources, kept as the porting reference (not compiled)
libs/       mcef-api.jar — compile-time dependency on MCEF
```

## Building

Requires JDK 17 and Gradle 8.8.

```bash
# MCEF must be built first, only its API jar is needed
cd ../mcef && ./gradlew :fabric:apiJar && cp fabric/build/libs/mcef-1.20.1-1.20-api.jar ../webdisplays/libs/mcef-api.jar

cd ../webdisplays
./gradlew build
```

## Configuration

`config/webdisplays.json`, written on first launch. Same keys as the 1.12.2 `.cfg`:

| Key | Default | Meaning |
|---|---|---|
| `blacklist` | `[]` | Domains that are never loaded |
| `homepage` | `mod://webdisplays/main.html` | URL loaded when a screen is created |
| `maxResolutionX` / `maxResolutionY` | 1920 / 1080 | Clamp for screen resolution |
| `maxScreenSizeX` / `maxScreenSizeY` | 16 / 16 | Maximum screen size in blocks |
| `miniservPort` | 25566 | Port of the built-in file host, 0 to disable |
| `loadDistance` / `unloadDistance` | 30 / 32 | Screen load radius |
| `enableAutoVolume` | `true` | Scale video volume with distance |
| `hardRecipes` | `true` | Require breaking a minePad to craft upgrades |
| `disableOwnershipThief` | `false` | Disable the ownership thief item |

## License

LGPL-2.1, inherited from upstream. See `LICENSE`.
