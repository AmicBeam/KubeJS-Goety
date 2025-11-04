package com.kubejs.goety.util;

import net.minecraft.world.item.crafting.Ingredient;

/**
 * 简单的 SizedIngredient 包装类
 * 用于包装 Ingredient 和数量
 */
public class SizedIngredient {
    private final Ingredient ingredient;
    private final int count;

    private SizedIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public static SizedIngredient of(Ingredient ingredient, int count) {
        return new SizedIngredient(ingredient, count);
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public int count() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SizedIngredient that = (SizedIngredient) o;
        return count == that.count && ingredient.equals(that.ingredient);
    }

    @Override
    public int hashCode() {
        return ingredient.hashCode() * 31 + count;
    }
}

