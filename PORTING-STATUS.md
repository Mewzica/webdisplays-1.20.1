# Porting status — WebDisplays 1.12.2 → 1.20.1

The upstream repositories (`webdisplays-1.20.1`, `mcef-1.20.1`) actually contained the **1.12.2 Forge**
sources despite their names. This document tracks the real port.

Version numbers were kept: `webdisplays 1.1` → `1.20.1-1.1`, `mcef 1.20` → `1.20.1-1.20`.

## What is done

### Build

* Single repository, Architectury Loom 1.7.416, Gradle 8.8, three loader modules sharing `common/`.
* All three loaders compile and package: `gradle build` produces 9 jars (3 mods + 3 sources + API jar
  for MCEF).
* `libs/mcef-api.jar` is a compile-only dependency, so WebDisplays builds without MCEF's full sources.

### Architecture

The 1.12.2 code was one `@Mod` class with a proxy, a config, a network channel, a coremod and 17 message
classes. None of that exists on 1.20.1, so the port introduces a small SPI
(`net.montoyo.wd.platform`) implemented once per loader:

| Interface | Purpose | Fabric | Forge / NeoForge |
|---|---|---|---|
| `WDRegistrar` | blocks, items, block entities, menus, sounds | `Registry.register` | `DeferredRegister` |
| `WDNet` | packet channel | `ServerPlayNetworking` (legacy channels) | `SimpleChannel` |
| `WDPlatform` | lifecycle events, game dir, server handle | Fabric API events | `MinecraftForge.EVENT_BUS` |

Common code never imports a loader class, which is why the same sources build for all three.

### Content ported

| Area | State |
|---|---|
| `ScreenBlockEntity` (was `TileEntityScreen`, 1084 lines) | Ported: screens, URL, resolution, rotation, ownership, friend list, rights, upgrades, redstone I/O, JS requests, laser mouse, auto volume |
| `ScreenBlock` (400 lines) | Ported: multiblock creation/validation, hit → pixel mapping, upgrade insertion, destruction handling, redstone emission |
| `PeripheralBlock` + `PeripheralBlockEntity` | Ported as one block + one block entity with a `kind` block state property (6 variants) |
| Items | `screen_configurator`, `linker`, `minepad`, `laser_pointer`, `ownership_thief`, `upgrade` (4 variants), `craft_component` (9 variants), 6 peripheral block items |
| Networking | 9 packets replacing the original 17; screen mutations and GUI opening use discriminators |
| `miniserv` file host | Ported unchanged (pure Java, no Minecraft imports) |
| Client renderer | Screens drawn from a world render callback with a `PoseStack`; browsers created lazily per screen |
| GUIs | `UrlScreen`, `ScreenConfigScreen`, `MinePadScreen`, `RedstoneCtrlScreen`, rebuilt on vanilla widgets |
| Assets | Textures, block models, blockstates, sounds, `mod://` HTML pages, 3 languages converted to 1.20.1 layout |

## Verification

What was actually run, not just compiled:

| Check | Result |
|---|---|
| `gradle build` on all three loaders | passes, 6 jars produced |
| `verify_assets.py` (registry ↔ resources) | passes: blockstates, model parents, textures, item models, translation keys, sound events |
| `:fabric:runServer` | `Done (11.8s)`, mod loaded as `webdisplays 1.20.1-1.1` |
| `:forge:runServer` | `Done (13.2s)`, mod loaded as `webdisplays 1.20.1-1.1` |
| `:neoforge:runServer` | `Done (13.3s)`, mod loaded as `webdisplays 1.20.1-1.1` |
| Screen block + block entity (RCON) | `setblock … screen[haste=true]` → `data get block` shows `WDScreens: []` |
| Peripheral variants (RCON) | all four implemented kinds place and serialise (`Channel0..3`, `PadId`) |
| Block entity lifecycle (RCON) | placing and removing screens/peripherals, redstone signal into the controller, and TNT destroying a screen all run without a stack trace |
| `config/webdisplays.json` | generated on first launch with the 1.12.2 key names |
| `:fabric:runClient` | **not verifiable here**: this shell has no display, so MCEF's Swing update window throws `java.awt.HeadlessException` before the main menu. Needs a desktop session. |

The client run is the one gap: model baking, GUI rendering and the CEF browser have not been seen
in a real window. Everything reachable from the server side has been exercised.

### Bugs the verification caught

Six defects that compiled cleanly but broke at runtime, all fixed:

1. **Fabric platform loaded client classes on the server.** `WebDisplaysFabricPlatform` and
   `WebDisplaysFabricNet` referenced `ClientTickEvents`, `WorldRenderEvents` and `ClientPlayNetworking`
   directly, so the dedicated server died in class verification with
   `Cannot load class net.minecraft.client.player.LocalPlayer in environment type SERVER`. The client
   half now lives in `WebDisplaysFabricClientEvents` and `WebDisplaysFabricNetClient`, which only the
   client entrypoint instantiates; the network layer replays the receivers registered before it existed.
2. **Sound event ids contained capitals.** `keyboardType`, `upgradeAdd`, `upgradeDel` and
   `screencfgOpen` are not valid `ResourceLocation` paths on 1.20.1. Renamed to `keyboard_type`,
   `upgrade_add`, `upgrade_del`, `screencfg_open` in both `WDContent` and `sounds.json`.
3. **Keyboard blockstate pointed at an item model.** `blockstates/peripheral.json` referenced
   `webdisplays:block/kb_inv`, which does not exist — `kb_inv` is an item model. It now uses
   `block/kb_left`, matching the 1.12.2 blockstate.
4. **Peripheral blockstates had no rotation.** All 24 variants used the same unrotated model, so every
   peripheral faced the same way. The `y` rotations from the 1.12.2 blockstate are restored.
5. **Two item models pointed at textures that do not exist.** `craft_component.json` asked for
   `item/craftcomp_peripheral` and `upgrade.json` for `item/craftcomp_upgrade`; the actual files are
   `item/peripheral` and `item/upgrade`.
6. **Every plain item was missing its translation key.** The language files still used the 1.12.2
   `item.webdisplays.<name>.name` form, which 1.20.1 does not read. The seven affected items now have
   proper `item.webdisplays.<name>` keys, and the dead `tile.*` keys were dropped.

## Deliberate design changes

These are not bugs; they are the places where a 1:1 translation is impossible or unwise.

1. **Item metadata → NBT.** 1.20.1 has no item metadata, so `upgrade` and `craft_component` store their
   variant under `WDUpgrade` / `WDPart`. Six peripheral variants became six items.
2. **One block entity type per block.** 1.12.2 registered one `TileEntity` class per peripheral; 1.20.1
   requires a single type per block, so the variant is a block state property.
3. **Custom widget toolkit dropped.** `client/gui/controls` (~2500 lines of hand-rolled widgets and a
   JSON-driven GUI loader) was replaced with vanilla `Button`/`EditBox`. The capabilities are the same,
   the look is not identical.
4. **`getRenderBoundingBox` unavailable.** Vanilla `BlockEntity` has no render bounding box, and Forge's
   version is loader-specific, so screens are drawn from a world render event over a tracked list
   instead of a `BlockEntityRenderer`.
5. **No capability for player data.** The 1.12.2 `IWDDCapability` only held a "first run" flag; the port
   does not register a capability at all, which keeps common code loader-agnostic.
6. **Advancement triggers removed.** The four criteria only gated `hardRecipes`. That check is now
   pending a simpler implementation (see below).
7. **Config moved from `.cfg` to JSON.** Forge `Configuration` is Forge-only; the same keys live in
   `config/webdisplays.json`.

## Not ported yet

Honest list of what the 1.20.1 build does **not** do:

* **ComputerCraft / OpenComputers integration.** Dropped by decision. `CC_INTERFACE` and `OC_INTERFACE`
  blocks still exist as inert placeholders so ordinals stay stable.
* **Recipes.** The 1.12.2 JSON recipes use the old format and were not converted, so nothing is
  craftable yet; items exist but have no recipe. This is the highest-value next task.
* **Advancements.** Removed with the criteria.
* **`hardRecipes` gating.** The config key is read but the "must break a minePad first" check is not
  enforced.
* **miniserv client.** The browser-side upload client was removed with its handshake packet; the server
  side of the file host still runs.
* **JS query dispatcher.** `webdisplaysRequest(...)` responses are logged, not routed back to the page.
* **`mod://` scheme registration.** The HTML assets ship in the jar, but `WDScheme` is not registered
  with MCEF yet, so `mod://webdisplays/main.html` will not resolve until that is wired up.
* **Block border model.** 1.12.2 used Forge unlisted block state properties to bake a per-face border;
  that mechanism does not exist on 1.20.1 and the border is currently not drawn.
* **Peripheral GUIs.** Keyboard and server peripherals show a chat message instead of their 1.12.2 GUIs.

## Next steps, in order of value

1. Convert the 1.12.2 recipes to 1.20.1 format (`legacy/src/main/resources/assets/webdisplays/recipes`).
2. Register the `mod://` scheme with MCEF and make the built-in pages load.
3. Re-implement `hardRecipes` using a per-player flag instead of an advancement.
4. Port `JSQueryDispatcher` so redstone output and JS callbacks round-trip.
5. Convert the block border to a `BlockEntityRenderer` with a custom model.
