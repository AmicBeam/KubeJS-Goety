package com.kubejs.goety.util;

import com.kubejs.goety.plugin.GoetyKubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.SizedIngredientWrapper;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** Normalizes script inputs while retaining their ingredient count. */
public final class ScriptIngredient {
    public final Ingredient ingredient;
    public final int count;

    private ScriptIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public static ScriptIngredient of(Object value) {
        while (value instanceof Wrapper wrapper) value = wrapper.unwrap();
        if (value == null) return null;
        if (value instanceof ScriptIngredient input) return input;
        if (value instanceof SizedIngredient input) return new ScriptIngredient(input.ingredient(), input.count());
        if (value instanceof net.neoforged.neoforge.common.crafting.SizedIngredient input) {
            return new ScriptIngredient(input.ingredient(), input.count());
        }
        if (value instanceof ItemStack stack) return new ScriptIngredient(Ingredient.of(stack), stack.getCount());
        if (value instanceof Ingredient ingredient) return new ScriptIngredient(ingredient, 1);
        if (value instanceof ItemLike item) return new ScriptIngredient(Ingredient.of(item), 1);
        var input = SizedIngredientWrapper.wrap(GoetyKubeJSPlugin.getServerContext(), value);
        return new ScriptIngredient(input.ingredient(), input.count());
    }

    public boolean isEmpty() {
        return count < 1 || ingredient.isEmpty();
    }
}
