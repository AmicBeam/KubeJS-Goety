package com.kubejs.goety.recipe.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;

public class PulverizeRecipeJS extends RecipeJS {
    
    @Override
    public JsonElement writeInputItem(InputItem value) {
        JsonElement defaultJson = value.ingredient.toJson();
        if (value.count > 1) {
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("count", value.count);
            wrapper.add("ingredient", defaultJson);
            return wrapper;
        }
        
        return defaultJson;
    }
}
