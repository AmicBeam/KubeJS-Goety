package com.kubejs.goety.recipe.js;

import com.kubejs.goety.recipe.schema.CursedInfuserSchema;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;

public class CursedInfuserRecipeJS extends KubeRecipe {
    public CursedInfuserRecipeJS(RecipeKey[] keys, Object[] values) {
        super(keys, values);
    }

    @Override
    public JsonObject serializeJson() {
        var json = super.serializeJson();
        
        json.add("ingredient", getValue(CursedInfuserSchema.INGREDIENT).toJson());
        
        var result = getValue(CursedInfuserSchema.RESULT);
        if (result != null && !result.isEmpty()) {
            // result 在 JSON 中是字符串（物品 ID）
            json.addProperty("result", result);
        }
        
        json.addProperty("cookingTime", getValue(CursedInfuserSchema.COOKING_TIME));
        
        var grim = getValue(CursedInfuserSchema.GRIM);
        if (grim != null && (Boolean) grim) {
            json.addProperty("grim", true);
        }
        
        return json;
    }
}

