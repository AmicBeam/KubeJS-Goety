package com.kubejs.goety.recipe.js;

import com.kubejs.goety.recipe.schema.SoulAbsorberSchema;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;

public class SoulAbsorberRecipeJS extends KubeRecipe {
    public SoulAbsorberRecipeJS(RecipeKey[] keys, Object[] values) {
        super(keys, values);
    }

    @Override
    public JsonObject serializeJson() {
        var json = super.serializeJson();

        json.add("ingredient", getValue(SoulAbsorberSchema.INGREDIENT).toJson());
        json.addProperty("soulIncrease", getValue(SoulAbsorberSchema.SOUL_INCREASE));
        json.addProperty("cookingtime", getValue(SoulAbsorberSchema.COOKING_TIME));

        return json;
    }
}

