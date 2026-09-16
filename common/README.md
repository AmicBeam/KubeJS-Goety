# Shared resources

This directory holds only assets that are identical across Minecraft versions.

Current shared files:

- `src/main/resources/logo.png`
- `src/main/resources/assets/kubejs_goety/lang/en_us.json`
- `src/main/resources/assets/kubejs_goety/lang/zh_cn.json`

Do not put version-specific Java, mixins, KubeJS plugin registration, `mods.toml` / `neoforge.mods.toml`, `pack.mcmeta`, or example scripts here. Those stay in `versions/<mc>/`.

Each version Gradle project copies these resources into its jar. `NOTICE.md` and `LICENSE.txt` are packaged from the repository root, not from this directory.
