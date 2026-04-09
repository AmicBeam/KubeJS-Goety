package com.kubejs.goety.recipe.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;

public class BrewingRecipeJS extends RecipeJS {
    
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

    @Override
    public void serialize() {
        super.serialize();
        if (json == null) {
            return;
        }
        moveToObject("entity", "entity_type", "entity.entity_type");
        moveToObject("entity", "tag", "entity.tag");
    }

    private void moveToObject(String objectKey, String fieldKey, String flatKey) {
        if (json.has(flatKey)) {
            JsonObject obj = json.has(objectKey) && json.get(objectKey).isJsonObject()
                    ? json.getAsJsonObject(objectKey)
                    : new JsonObject();
            obj.add(fieldKey, json.get(flatKey));
            json.remove(flatKey);
            json.add(objectKey, obj);
        }
    }
}
