# Agent instructions — two-version layout

This repository is a multi-version workspace. There is **no root Gradle project** and no implicit default Minecraft version.

## Layout

- `versions/1.20.1/` — supported Forge 1.20.1 implementation. Independent Gradle project (wrapper, `build.gradle`, `settings.gradle`).
- `versions/1.21.1/` — independent NeoForge 1.21.1 development scaffold. Feature port pending.
- `common/` — only truly shared resources (currently logo + lang). Not a Gradle project.
- Root `NOTICE.md` and `LICENSE.txt` stay at the repository root and must be packaged into each version jar.

## Working rules

- Run Gradle from the version directory you are changing (`versions/1.20.1` or `versions/1.21.1`). Never assume a root `./gradlew`.
- Preserve existing 1.20.1 behavior when adapting 1.21.1. Keep loader and dependency API differences in their version directories.
- When porting features, adapt and validate the KubeJS plugin, recipe APIs, event hooks and mixin targets against that version's actual dependencies.
- Keep version-specific metadata (`mods.toml` / `neoforge.mods.toml`, `pack.mcmeta`, mixins JSON, example scripts) inside that version tree.
- Put a file in `common/` only when it is identical and intended for every version jar.
- Jar names must include `+<minecraft version>` (example: `kubejs_goety-1.1.1+1.20.1.jar`). Read current mod and dependency versions from each project's `gradle.properties`.
- You are not alone in this repository. Do not revert concurrent edits outside the assigned version/module scope.

## Validation

- Build 1.20.1 with Java 17 and 1.21.1 with Java 21. Shared-resource changes require both builds.
- Distinguish successful compilation from in-game verification. Keep the 1.21.1 feature status in its README accurate as the port progresses.
- Dependencies are compile-only; development runs need their runtime jars and required dependencies in the corresponding `run/mods/`.
