package com.kubejs.goety.compat;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compatibility decisions needed before normal Forge mod construction.
 */
public final class EarlyModCompatibility {
    private static final Logger LOGGER = LogManager.getLogger("KubeJS Goety Compatibility");
    private static final List<Conflict> CAULDRON_CONFLICTS = detectCauldronConflicts();

    private EarlyModCompatibility() {
    }

    public static boolean shouldDisableCauldronMixins() {
        return !CAULDRON_CONFLICTS.isEmpty();
    }

    public static List<Conflict> getCauldronConflicts() {
        return CAULDRON_CONFLICTS;
    }

    public static String getConflictSummary() {
        return CAULDRON_CONFLICTS.stream()
                .map(conflict -> conflict.displayName() + " " + conflict.version())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    public static void logDecision() {
        if (shouldDisableCauldronMixins()) {
            LOGGER.warn("Detected incompatible cauldron mixins from {}. KubeJS Goety is disabling its three "
                            + "cauldron integration mixins to prevent a startup crash. Ritual, recipe, and other "
                            + "brew registration features remain enabled.",
                    getConflictSummary());
        }
    }

    private static List<Conflict> detectCauldronConflicts() {
        List<Conflict> conflicts = new ArrayList<>();
        findMod("revelationfix").filter(mod -> isAtMost(mod, 4, 4, 0))
                .ifPresent(mod -> conflicts.add(new Conflict("revelationfix", "RevelationFix", versionOf(mod))));
        findMod("goetyawaken").filter(mod -> isAtMost(mod, 1, 3, 8))
                .ifPresent(mod -> conflicts.add(new Conflict("goetyawaken", "Goety Awaken", versionOf(mod))));
        return Collections.unmodifiableList(conflicts);
    }

    private static java.util.Optional<IModInfo> findMod(String modId) {
        try {
            LoadingModList loadingModList = FMLLoader.getLoadingModList();
            if (loadingModList == null) {
                return java.util.Optional.empty();
            }
            return loadingModList.getModFiles().stream()
                    .flatMap(modFileInfo -> modFileInfo.getMods().stream())
                    .filter(mod -> modId.equals(mod.getModId()))
                    .findFirst();
        } catch (Throwable throwable) {
            LOGGER.warn("Unable to inspect early mod metadata for {}", modId, throwable);
            return java.util.Optional.empty();
        }
    }

    private static boolean isAtMost(IModInfo mod, int major, int minor, int incremental) {
        try {
            int detectedMajor = mod.getVersion().getMajorVersion();
            int detectedMinor = mod.getVersion().getMinorVersion();
            int detectedIncremental = mod.getVersion().getIncrementalVersion();
            if (detectedMajor != major) {
                return detectedMajor < major;
            }
            if (detectedMinor != minor) {
                return detectedMinor < minor;
            }
            return detectedIncremental <= incremental;
        } catch (Throwable throwable) {
            // An unparseable version of a known conflicting mod is safer in fallback mode.
            LOGGER.warn("Unable to parse version '{}' for {}; applying the safe cauldron fallback",
                    versionOf(mod), mod.getModId());
            return true;
        }
    }

    private static String versionOf(IModInfo mod) {
        try {
            return mod.getVersion().toString();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public record Conflict(String modId, String displayName, String version) {
    }
}
