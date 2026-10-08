package com.kubejs.goety.brew;

import net.minecraft.core.NonNullList;

/** Grows brewing storage without discarding items or shrinking addon storage. */
public final class BrewInventory {
    private BrewInventory() {
    }

    public static <T> NonNullList<T> grow(NonNullList<T> items, int requiredSize, T empty) {
        if (items.size() >= requiredSize) {
            return items;
        }
        NonNullList<T> grown = NonNullList.withSize(requiredSize, empty);
        for (int i = 0; i < items.size(); i++) {
            grown.set(i, items.get(i));
        }
        return grown;
    }
}
