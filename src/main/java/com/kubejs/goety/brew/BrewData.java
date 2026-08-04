package com.kubejs.goety.brew;

import net.minecraft.world.item.Item;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrewData {
    public static final int MAX_CAULDRON_CAPACITY = 32;
    public static final int MAX_CAPACITY_LEVEL = 7;
    // The initial two-index layout referenced RevelationFix's BrewData; see NOTICE.md.
    public static final Map<Integer, List<Item>> LEVEL_TO_CAPACITY_ITEMS = createLevelMap(MAX_CAPACITY_LEVEL);
    public static final Map<String, Map<Integer, List<Item>>> TYPE_TO_LEVEL_AUGMENT_ITEMS = createAugmentMap();
    private static List<Integer> CAPACITY_LEVEL_DELTAS = new ArrayList<>();
    private static Integer INITIAL_CAPACITY_OVERRIDE;
    private static Item CAULDRON_STARTER;
    private static final int DEFAULT_INITIAL_CAPACITY = 4;
    private static final int[] DEFAULT_LEVEL_DELTAS = new int[]{2, 2, 2, 2, 4, 6};
    private static final Map<String, List<AugmentationLevel>> DEFAULT_AUGMENT_LEVELS = createDefaultAugmentLevels();
    private static final Map<String, List<AugmentationLevel>> AUGMENT_LEVELS = new HashMap<>(DEFAULT_AUGMENT_LEVELS);

    private static Map<Integer, List<Item>> createLevelMap(int maxLevel) {
        Map<Integer, List<Item>> map = new HashMap<>();
        for (int i = 0; i <= maxLevel; i++) {
            map.put(i, new ArrayList<>());
        }
        return map;
    }

    private static Map<String, Map<Integer, List<Item>>> createAugmentMap() {
        Map<String, Map<Integer, List<Item>>> map = new HashMap<>();
        map.put("duration", new HashMap<>());
        map.put("amplifier", new HashMap<>());
        map.put("aoe", new HashMap<>());
        map.put("linger", new HashMap<>());
        map.put("quaff", new HashMap<>());
        map.put("velocity", new HashMap<>());
        map.put("aquatic", createLevelMap(0));
        map.put("fire_proof", createLevelMap(0));
        map.put("hidden", createLevelMap(0));
        map.put("splash", createLevelMap(0));
        map.put("lingering", createLevelMap(0));
        map.put("gas", createLevelMap(0));
        return map;
    }

    private static Map<String, List<AugmentationLevel>> createDefaultAugmentLevels() {
        Map<String, List<AugmentationLevel>> map = new HashMap<>();
        map.put("duration", List.of(
                new AugmentationLevel(1.0F, 1.25F),
                new AugmentationLevel(1.0F, 1.5F),
                new AugmentationLevel(1.0F, 2.0F)
        ));
        map.put("amplifier", List.of(
                new AugmentationLevel(1.0F, 2.0F),
                new AugmentationLevel(1.0F, 2.5F),
                new AugmentationLevel(1.0F, 3.0F)
        ));
        map.put("aoe", List.of(
                new AugmentationLevel(1.0F, 1.25F),
                new AugmentationLevel(1.0F, 1.5F),
                new AugmentationLevel(1.0F, 2.0F)
        ));
        map.put("linger", List.of(
                new AugmentationLevel(1.0F, 1.25F),
                new AugmentationLevel(1.0F, 1.25F),
                new AugmentationLevel(1.0F, 1.25F)
        ));
        map.put("quaff", List.of(
                new AugmentationLevel(8.0F, 1.25F),
                new AugmentationLevel(8.0F, 1.25F),
                new AugmentationLevel(8.0F, 1.25F)
        ));
        map.put("velocity", List.of(
                new AugmentationLevel(0.1F, 1.25F),
                new AugmentationLevel(0.2F, 1.25F),
                new AugmentationLevel(0.2F, 1.25F)
        ));
        return map;
    }

    public static void registerCapacity(Item item, int level) {
        List<Item> list = LEVEL_TO_CAPACITY_ITEMS.computeIfAbsent(level, k -> new ArrayList<>());
        list.remove(item);
        list.add(item);
    }

    public static void setCapacityLevelDeltas(List<Integer> deltas) {
        if (deltas == null) {
            CAPACITY_LEVEL_DELTAS = new ArrayList<>();
            return;
        }
        CAPACITY_LEVEL_DELTAS = new ArrayList<>(deltas);
    }

    public static void setAugmentationLevels(String type, List<AugmentationLevel> levels) {
        if (!isLevelableAugmentation(type) || levels == null) {
            return;
        }
        AUGMENT_LEVELS.put(type, new ArrayList<>(levels));
    }

    public static boolean isLevelableAugmentation(String type) {
        return DEFAULT_AUGMENT_LEVELS.containsKey(type);
    }

    public static AugmentationLevel getAugmentationLevel(String type, int level) {
        if (level < 0) {
            return null;
        }
        List<AugmentationLevel> levels = AUGMENT_LEVELS.get(type);
        if (levels == null || level >= levels.size()) {
            return null;
        }
        return levels.get(level);
    }

    public static float getAugmentationValuePrefix(String type, int level) {
        if (level <= 0) {
            return 0.0F;
        }
        List<AugmentationLevel> levels = AUGMENT_LEVELS.get(type);
        if (levels == null) {
            return 0.0F;
        }
        float sum = 0.0F;
        int max = Math.min(level, levels.size());
        for (int i = 0; i < max; i++) {
            sum += levels.get(i).value();
        }
        return sum;
    }

    public static float getDefaultAugmentationCost(String type, int level) {
        List<AugmentationLevel> levels = DEFAULT_AUGMENT_LEVELS.get(type);
        if (levels == null || levels.isEmpty()) {
            return 1.25F;
        }
        if (level < levels.size()) {
            return levels.get(level).cost();
        }
        return levels.get(levels.size() - 1).cost();
    }

    public static int getCapacityDelta(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= CAPACITY_LEVEL_DELTAS.size()) {
            return CAPACITY_LEVEL_DELTAS.get(level - 1);
        }
        if (level >= 1 && level <= DEFAULT_LEVEL_DELTAS.length) {
            return readConfigInt("Level" + level + "Capacity", DEFAULT_LEVEL_DELTAS[level - 1]);
        }
        return 0;
    }

    public static int getCapacityPrefixSum(int level) {
        int sum = 0;
        for (int i = 1; i <= level; i++) {
            sum += getCapacityDelta(i);
        }
        return sum;
    }

    public static int getMaxCapacityLevel() {
        if (!CAPACITY_LEVEL_DELTAS.isEmpty()) {
            return CAPACITY_LEVEL_DELTAS.size();
        }
        return DEFAULT_LEVEL_DELTAS.length;
    }

    public static int getMaxCapacity() {
        return Math.min(MAX_CAULDRON_CAPACITY, getInitialCapacity() + getCapacityPrefixSum(getMaxCapacityLevel()));
    }

    public static int getInitialCapacity() {
        if (INITIAL_CAPACITY_OVERRIDE != null) {
            return Math.min(MAX_CAULDRON_CAPACITY, INITIAL_CAPACITY_OVERRIDE);
        }
        return Math.min(MAX_CAULDRON_CAPACITY, readConfigInt("InitialCapacity", DEFAULT_INITIAL_CAPACITY));
    }

    public static void setInitialCapacity(int value) {
        if (value <= 0) {
            return;
        }
        INITIAL_CAPACITY_OVERRIDE = value;
    }

    public static void setCauldronStarter(Item item) {
        CAULDRON_STARTER = item;
    }

    public static Item getCauldronStarter(Item fallback) {
        return CAULDRON_STARTER != null ? CAULDRON_STARTER : fallback;
    }

    private static int readConfigInt(String fieldName, int fallback) {
        try {
            Class<?> clazz = Class.forName("com.Polarice3.Goety.config.BrewConfig");
            Field field = clazz.getDeclaredField(fieldName);
            Object configValue = field.get(null);
            if (configValue == null) {
                return fallback;
            }
            Method getMethod = configValue.getClass().getMethod("get");
            Object value = getMethod.invoke(configValue);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    public static int removeCapacityItem(Item item) {
        int removed = 0;
        for (List<Item> list : LEVEL_TO_CAPACITY_ITEMS.values()) {
            if (list.remove(item)) {
                removed++;
            }
        }
        return removed;
    }

    public static void registerAugmentation(Item item, String type, int level) {
        Map<Integer, List<Item>> levelMap = TYPE_TO_LEVEL_AUGMENT_ITEMS.get(type);
        if (levelMap == null) {
            return;
        }
        List<Item> list = levelMap.computeIfAbsent(level, k -> new ArrayList<>());
        list.remove(item);
        list.add(item);
    }

    public static int removeAugmentationItem(Item item) {
        int removed = 0;
        for (Map<Integer, List<Item>> levelMap : TYPE_TO_LEVEL_AUGMENT_ITEMS.values()) {
            for (List<Item> list : levelMap.values()) {
                if (list.remove(item)) {
                    removed++;
                }
            }
        }
        return removed;
    }

    public static int removeModifierItem(Item item) {
        return removeCapacityItem(item) + removeAugmentationItem(item);
    }

    public record AugmentationLevel(float value, float cost) {
    }
}
