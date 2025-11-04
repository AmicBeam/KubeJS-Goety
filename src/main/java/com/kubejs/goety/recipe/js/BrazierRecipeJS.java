package com.kubejs.goety.recipe.js;

import com.kubejs.goety.recipe.schema.BrazierSchema;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.util.ListJS;

import java.util.List;

public class BrazierRecipeJS extends KubeRecipe {
    public BrazierRecipeJS(RecipeKey[] keys, Object[] values) {
        super(keys, values);
    }

    @Override
    public JsonObject serializeJson() {
        var json = super.serializeJson();
        
        json.add("result", getValue(BrazierSchema.RESULT).toJson());
        
        var ingredients = getValue(BrazierSchema.INGREDIENTS);
        var ingredientsArray = new JsonArray();
        if (ingredients != null) {
            List<InputItem> ingredientsList = ListJS.of(ingredients);
            if (ingredientsList != null) {
                for (var ingredient : ingredientsList) {
                    if (ingredient != null && !ingredient.isEmpty()) {
                        ingredientsArray.add(ingredient.toJson());
                    }
                }
            }
        }
        json.add("ingredients", ingredientsArray);
        
        json.addProperty("soulCost", getValue(BrazierSchema.SOUL_COST));
        
        return json;
    }
}

