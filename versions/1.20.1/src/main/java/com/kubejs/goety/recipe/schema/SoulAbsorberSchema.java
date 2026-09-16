package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.SoulAbsorberRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface SoulAbsorberSchema {
    RecipeKey<InputItem> INGREDIENT = ItemComponents.INPUT.key("ingredient");
    RecipeKey<Integer> SOUL_INCREASE = NumberComponent.INT.key("soulIncrease").optional(25).preferred("soulIncrease");
    RecipeKey<Integer> COOKING_TIME = NumberComponent.INT.key("cookingTime").optional(200).preferred("cookingTime");

    RecipeSchema SCHEMA = new RecipeSchema(SoulAbsorberRecipeJS.class, SoulAbsorberRecipeJS::new,
            INGREDIENT, SOUL_INCREASE, COOKING_TIME)
            .constructor(INGREDIENT);
}

