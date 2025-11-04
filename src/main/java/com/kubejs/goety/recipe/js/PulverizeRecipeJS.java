package com.kubejs.goety.recipe.js;

import com.kubejs.goety.recipe.schema.PulverizeSchema;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;

public class PulverizeRecipeJS extends KubeRecipe {
    public PulverizeRecipeJS(RecipeKey[] keys, Object[] values) {
        super(keys, values);
    }

    @Override
    public JsonObject serializeJson() {
        var json = super.serializeJson();
        
        var ingredient = getValue(PulverizeSchema.INGREDIENT);
        if (ingredient != null && !ingredient.isEmpty()) {
            json.add("ingredient", ingredient.toJson());
        }
        
        var blockResult = getValue(PulverizeSchema.BLOCK_RESULT);
        var itemResult = getValue(PulverizeSchema.ITEM_RESULT);
        
        if (blockResult != null && !blockResult.isEmpty()) {
            json.addProperty("block_result", blockResult);
        }
        
        if (itemResult != null && !itemResult.isEmpty()) {
            json.add("item_result", itemResult.toJson());
        }
        
        return json;
    }
}

