# KubeJS Goety — Minecraft 1.21.1

Status: **NeoForge beta port implemented. Dedicated-server validation passed; client playtesting pending.**

This is an independent Java 21 / NeoForge project. The Forge 1.20.1 implementation remains unchanged.

## Versions

| Dependency | Validated version |
| --- | --- |
| Minecraft / Java | 1.21.1 / 21 |
| NeoForge | 21.1.234 |
| KubeJS NeoForge | 2101.7.2-build.377 |
| Rhino | 2101.2.7-build.81 |
| Goety | 3.1.4 (Modrinth file `LBDrMg4p`) |
| Curios / Patchouli (runtime) | 9.5.1+1.21.1 / 1.21.1-93-NEOFORGE |

## Implemented

- KubeJS plugin, `GoetyEvents` registration/modification/removal, ritual conditions and start/finish callbacks.
- Custom Research, actual Goety ResearchScroll items, prerequisites, player API and client-bound definition payloads.
- Brew effects, catalysts, capacity/augmentation tables and grow-only inventories up to 256 slots.
- Seven recipe schemas: ritual, brewing, cauldron, pulverize, cursed infuser, brazier and soul absorber.
- NeoForge event registration and payload networking, Minecraft 1.21 components/codecs, and Goety 3.1.4 Mixin targets.
- Script reload support. Runtime callback contexts are replaced with new bindings and released when the server stops.

Script method names follow the root documentation. Use this version's [example scripts](src/main/resources/kubejs), which use NeoForge `c:` tags. Java types changed from legacy InputItem/OutputItem to ingredients, sized ingredients and ItemStack.

## Build

Run with Java 21 from this directory:

```bash
./gradlew build
```

Output: `build/libs/kubejs_goety-1.1.1+1.21.1.jar`.

Shared logo/lang come from `../../common/src/main/resources`; root `NOTICE.md` and `LICENSE.txt` are packaged into the jar. Dependencies are compile-only and are not bundled. For `runClient` or `runServer`, install the runtime jars above into `run/mods/`.

## Validation

Verified on 2026-10-07:

- Complete build and standalone brew ownership/storage regression checks.
- Six cauldron injection selectors, nine constructor/invocation anchors (including Patchouli processors), and ritual/cauldron callback descriptors checked against the unmodified production Goety 3.1.4 jar.
- Research payload codec round-trip, including prerequisites, Unicode names and nullable fields.
- Dedicated-server startup with custom scrolls, all five Goety events and ten decoded recipes covering seven types and omitted optional arguments.
- Custom ritual condition and start/finish callbacks.
- Real `goety:witch_cauldron` capacity 4 → 36 → 68, preserving an ingredient in slot 67 through save/load.
- `/reload` rebuilds Research and recipes, and custom ritual callbacks still have a valid script context afterward.

The [migration smoke fixtures](src/test/resources/migration-smoke) reproduce these server checks. Copy their startup/server script directories into an isolated test instance's `kubejs/`, install the runtime dependencies, and run the server. They place a test cauldron at `(0, 200, 0)`, reload once, and automatically stop the server. Success requires all four `KJS_GOETY_*_PASS` markers and no script errors; a successful Gradle exit alone is insufficient because Minecraft may report a loading failure and still exit with code 0.

Client UI/JEI rendering, multiplayer login and live client synchronization, and coexistence with third-party Goety addons still need in-game validation. The port is therefore marked beta rather than a fully playtested release.
