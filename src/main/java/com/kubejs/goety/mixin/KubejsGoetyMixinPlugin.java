package com.kubejs.goety.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class KubejsGoetyMixinPlugin implements IMixinConfigPlugin {
    private static final String CONFIG_FILE = "kubejs_goety.properties";
    private static final String SKIP_REVELATION_CAULDRON_KEY = "skipCauldronBrewingMixinsWithRevelation";
    private static final boolean DEFAULT_SKIP_REVELATION_CAULDRON = true;
    private static final String DEFAULT_CONFIG_CONTENT = String.join(System.lineSeparator(),
            "# KubeJS Goety early mixin options.",
            "# This file is read during mixin loading. Restart Minecraft after changing it.",
            "# true: skip KubeJS Goety cauldron brewing mixins when Goety: Revelation is loaded.",
            "# false: force KubeJS Goety cauldron brewing mixins to apply anyway.",
            SKIP_REVELATION_CAULDRON_KEY + "=" + DEFAULT_SKIP_REVELATION_CAULDRON,
            "");

    @Override
    public void onLoad(String mixinPackage) {
        createDefaultConfigIfMissing(getEarlyConfigPath());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (isCauldronBrewingMixin(mixinClassName)
                && isRevelationLoaded()
                && shouldSkipCauldronBrewingMixinsWithRevelation()) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private boolean isCauldronBrewingMixin(String mixinClassName) {
        return mixinClassName.endsWith("BrewCauldronBlockEntityMixin")
                || mixinClassName.endsWith("BrewCauldronCapacityMixin")
                || mixinClassName.endsWith("BrewCauldronCraftingStarterMixin");
    }

    private boolean isRevelationLoaded() {
        return isModLoadedEarly("revelationfix") || isModLoadedEarly("goety_revelation");
    }

    private boolean shouldSkipCauldronBrewingMixinsWithRevelation() {
        Boolean propertyOverride = parseBoolean(System.getProperty("kubejs_goety." + SKIP_REVELATION_CAULDRON_KEY));
        if (propertyOverride != null) {
            return propertyOverride;
        }

        Path configPath = getEarlyConfigPath();
        if (configPath == null || !Files.isRegularFile(configPath)) {
            createDefaultConfigIfMissing(configPath);
            return DEFAULT_SKIP_REVELATION_CAULDRON;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException ignored) {
            return DEFAULT_SKIP_REVELATION_CAULDRON;
        }

        Boolean configured = parseBoolean(properties.getProperty(SKIP_REVELATION_CAULDRON_KEY));
        return configured != null ? configured : DEFAULT_SKIP_REVELATION_CAULDRON;
    }

    private Path getEarlyConfigPath() {
        try {
            Path gamePath = FMLLoader.getGamePath();
            if (gamePath != null) {
                return gamePath.resolve("config").resolve(CONFIG_FILE);
            }
        } catch (Throwable ignored) {
        }
        return Path.of("config", CONFIG_FILE);
    }

    private void createDefaultConfigIfMissing(Path configPath) {
        if (configPath == null || Files.exists(configPath)) {
            return;
        }

        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(configPath, DEFAULT_CONFIG_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (Throwable ignored) {
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null) {
            return null;
        }

        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true":
            case "yes":
            case "on":
            case "1":
                return true;
            case "false":
            case "no":
            case "off":
            case "0":
                return false;
            default:
                return null;
        }
    }

    private boolean isModLoadedEarly(String modId) {
        try {
            LoadingModList loadingModList = FMLLoader.getLoadingModList();
            if (loadingModList == null) {
                return false;
            }
            return loadingModList.getModFiles().stream()
                    .flatMap(modFileInfo -> modFileInfo.getMods().stream())
                    .map(IModInfo::getModId)
                    .anyMatch(modId::equals);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
