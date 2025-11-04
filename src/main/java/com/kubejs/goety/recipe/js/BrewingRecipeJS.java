package com.kubejs.goety.recipe.js;

import com.kubejs.goety.recipe.schema.BrewingSchema;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;

public class BrewingRecipeJS extends KubeRecipe {
    public BrewingRecipeJS(RecipeKey[] keys, Object[] values) {
        super(keys, values);
    }

    @Override
    public JsonObject serializeJson() {
        var json = super.serializeJson();
        
        json.add("ingredient", getValue(BrewingSchema.INGREDIENT).toJson());
        json.addProperty("effect", getValue(BrewingSchema.EFFECT));
        json.addProperty("soulCost", getValue(BrewingSchema.SOUL_COST));
        json.addProperty("capacityExtra", getValue(BrewingSchema.CAPACITY_EXTRA));
        json.addProperty("duration", getValue(BrewingSchema.DURATION));
        
        // 添加可选的 entity 对象
        var entityType = getValue(BrewingSchema.ENTITY_TYPE);
        var entityTag = getValue(BrewingSchema.ENTITY_TAG);
        if (entityType != null && !entityType.isEmpty()) {
            var entityObj = new JsonObject();
            entityObj.addProperty("entity_type", entityType);
            json.add("entity", entityObj);
        } else if (entityTag != null && !entityTag.isEmpty()) {
            var entityObj = new JsonObject();
            entityObj.addProperty("tag", entityTag);
            json.add("entity", entityObj);
        }
        
        return json;
    }
}

