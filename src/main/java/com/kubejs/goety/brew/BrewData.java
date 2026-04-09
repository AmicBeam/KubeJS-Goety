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
    public static final int MAX_AUGMENT_LEVEL = 4;
    public static final Map<Integer, List<Item>> LEVEL_TO_CAPACITY_ITEMS = createLevelMap(MAX_CAPACITY_LEVEL);
    public static final Map<String, Map<Integer, List<Item>>> TYPE_TO_LEVEL_AUGMENT_ITEMS = createAugmentMap();
    private static List<Integer> CAPACITY_LEVEL_DELTAS = new ArrayList<>();
    private static Integer INITIAL_CAPACITY_OVERRIDE;
    private static final int DEFAULT_INITIAL_CAPACITY = 4;
    private static final int[] DEFAULT_LEVEL_DELTAS = new int[]{2, 2, 2, 2, 4};

    private static Map<Integer, List<Item>> createLevelMap(int maxLevel) {
        Map<Integer, List<Item>> map = new HashMap<>();
        for (int i = 0; i <= maxLevel; i++) {
            map.put(i, new ArrayList<>());
        }
        return map;
    }

    private static Map<String, Map<Integer, List<Item>>> createAugmentMap() {
        Map<String, Map<Integer, List<Item>>> map = new HashMap<>();
        map.put("duration", createLevelMap(MAX_AUGMENT_LEVEL));
        map.put("amplifier", createLevelMap(MAX_AUGMENT_LEVEL));
        map.put("aoe", createLevelMap(MAX_AUGMENT_LEVEL));
        map.put("linger", createLevelMap(MAX_AUGMENT_LEVEL));
        map.put("quaff", createLevelMap(MAX_AUGMENT_LEVEL));
        map.put("velocity", createLevelMap(MAX_AUGMENT_LEVEL));
        map.put("aquatic", createLevelMap(0));
        map.put("fire_proof", createLevelMap(0));
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
        return 5;
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
        List<Item> list = levelMap.get(level);
        if (list == null) {
            return;
        }
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
}
