package com.kubejs.goety.ritual;

import com.Polarice3.Goety.api.ritual.IRitualType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** KubeJS-owned ritual overrides, kept separate from registries other mods may rebuild. */
public final class RitualOverrides {
    private static final Map<String, IRitualType> OVERRIDES = new ConcurrentHashMap<>();

    private RitualOverrides() {
    }

    public static void put(String id, IRitualType ritualType) {
        OVERRIDES.put(id, ritualType);
    }

    public static IRitualType get(String id) {
        return OVERRIDES.get(id);
    }

    public static void remove(String id) {
        OVERRIDES.remove(id);
    }
}
