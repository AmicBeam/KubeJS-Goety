# KubeJS Goety — Minecraft 1.20.1

Status: **supported Forge implementation**. This is the current feature-complete codebase (rituals, brews, recipes, research, mixins).

- Loader: Forge 47.1.3 / Minecraft 1.20.1 / Java 17
- Mod version: `1.1.1` (jar name includes `+1.20.1`)
- Dependencies stay at the existing 1.20.1 coordinates in `gradle.properties`
- Shared logo/lang come from `../../common/src/main/resources`
- `NOTICE.md` and `LICENSE.txt` are packaged from the repository root

There is no repository-root Gradle project. Build only from this directory:

```bash
./gradlew build
```

Do not treat this tree as a default for 1.21.1 work. Leave 1.21.1 sources independent until a dedicated port task.
