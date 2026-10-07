package com.kubejs.goety.brew;

import java.util.ArrayList;
import java.util.List;

/** Regression checks for selective ownership, runnable without starting Minecraft. */
public final class BrewLevelOwnershipTest {
    public static void main(String[] args) {
        List<String> types = List.of("duration", "amplifier", "aoe", "linger", "quaff", "velocity");
        require(!BrewData.hasScriptedCapacityLevels(), "Capacity must initially use upstream rules");
        for (String type : types) {
            require(!BrewData.hasScriptedAugmentationLevels(type), type + " must initially use upstream rules");
        }

        List<Integer> capacityLevels = new ArrayList<>(List.of(2, 2, 2, 2, 4, 6, 8));
        BrewData.setCapacityLevelDeltas(capacityLevels);
        capacityLevels.clear();
        require(BrewData.hasScriptedCapacityLevels(), "Explicit capacity table must take ownership");
        require(BrewData.getMaxCapacityLevel() == 7, "Configured capacity levels must survive caller mutation");
        require(BrewData.getCapacityDelta(7) == 8, "Scripted capacity level 7 must be available");
        require(BrewData.getCapacityDelta(8) == 0, "Missing scripted capacity levels must not use defaults");
        for (String type : types) {
            require(!BrewData.hasScriptedAugmentationLevels(type), "Capacity must not take ownership of " + type);
        }

        BrewData.setCapacityLevelDeltas(List.of());
        require(BrewData.hasScriptedCapacityLevels(), "An explicit empty table must disable capacity upgrades");
        require(BrewData.getMaxCapacityLevel() == 0, "Empty table must not restore the default six levels");
        BrewData.setCapacityLevelDeltas(null);
        require(!BrewData.hasScriptedCapacityLevels(), "Clearing the override must restore upstream ownership");

        List<BrewData.AugmentationLevel> durationLevels = new ArrayList<>(List.of(
                new BrewData.AugmentationLevel(1.0F, 1.25F),
                new BrewData.AugmentationLevel(1.0F, 1.5F),
                new BrewData.AugmentationLevel(1.0F, 2.0F),
                new BrewData.AugmentationLevel(2.0F, 2.5F)));
        BrewData.setAugmentationLevels("duration", durationLevels);
        durationLevels.clear();
        require(BrewData.hasScriptedAugmentationLevels("duration"), "Explicit duration must take ownership");
        require(!BrewData.hasScriptedCapacityLevels(), "Duration must not take capacity ownership");
        for (String type : types.subList(1, types.size())) {
            require(!BrewData.hasScriptedAugmentationLevels(type), "Duration must not take ownership of " + type);
        }
        require(BrewData.getAugmentationLevel("duration", 3).value() == 2.0F,
                "Scripted augmentation level 3 must survive caller mutation");
        require(BrewData.getAugmentationValuePrefix("duration", 3) == 3.0F,
                "Scripted levels must keep their sequential requirements");
        require(BrewData.getAugmentationLevel("duration", 4) == null,
                "Explicit tables must reject levels beyond their configured range");
        require(BrewData.getDefaultAugmentationCost("duration", 3) == 2.0F,
                "Default cost fallback must remain available without claiming upstream levels");
        BrewData.setAugmentationLevels("unknown", durationLevels);
        require(!BrewData.hasScriptedAugmentationLevels("unknown"), "Unknown modifiers must not be claimed");
        BrewData.setCapacityLevelDeltas(List.of(2, 2));
        BrewData.resetScriptedLevelTables();
        require(!BrewData.hasScriptedCapacityLevels(), "A new registration cycle must release old capacity rules");
        for (String type : types) {
            require(!BrewData.hasScriptedAugmentationLevels(type), "A new registration cycle must release " + type);
        }
        System.out.println("Brew level ownership regression checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
