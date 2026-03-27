package com.kubejs.goety.brew;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrewData {
    public static final int MAX_CAPACITY_LEVEL = 7;
    public static final int MAX_AUGMENT_LEVEL = 4;
    public static final Map<Integer, List<Item>> LEVEL_TO_CAPACITY_ITEMS = createLevelMap(MAX_CAPACITY_LEVEL);
    public static final Map<String, Map<Integer, List<Item>>> TYPE_TO_LEVEL_AUGMENT_ITEMS = createAugmentMap();

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

    public static List<Item> replaceCapacity(Item item, int level) {
        List<Item> list = LEVEL_TO_CAPACITY_ITEMS.get(level);
        if (list == null) {
            return List.of();
        }
        List<Item> removed = new ArrayList<>(list);
        list.clear();
        list.add(item);
        removed.remove(item);
        return removed;
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
}
