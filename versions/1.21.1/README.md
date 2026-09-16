# KubeJS Goety — Minecraft 1.21.1

Status: **NeoForge development scaffold only. Feature port pending.**

This directory is an independent Gradle project for 1.21.1 development. It is not a port of the 1.20.1 feature set.

Included now:

- Gradle 8.14.3 wrapper
- `net.neoforged.moddev` 2.0.141
- NeoForge 21.1.234
- Java 21 / pack format 34
- Compile-only public deps: KubeJS NeoForge `2101.7.2-build.377`, Rhino `2101.2.7-build.81`, Goety `maven.modrinth:goety:LBDrMg4p` (3.1.4)
- Metadata requiring `kubejs` and `goety`
- Minimal `@Mod` entrypoint only
- Shared logo/lang from `../../common/src/main/resources`
- `NOTICE.md` and `LICENSE.txt` packaged from the repository root
- Jar name includes `+1.21.1`

Not included (pending a dedicated 1.21.1 port):

- 1.20.1 Java feature code
- KubeJS plugin registration
- Mixins / mixin plugin
- Recipe schemas, brew/research/ritual event APIs

Do not copy unported 1.20.1 sources into this tree. Build only from this directory:

```bash
./gradlew build
```

Dependencies above are compile-only. Before using `runClient` or `runServer`,
install their NeoForge 1.21.1 runtime jars and required dependencies into this
project's `run/mods/`. The scaffold does not provide the scripting APIs yet.
