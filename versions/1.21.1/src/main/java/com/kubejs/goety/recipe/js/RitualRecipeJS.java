package com.kubejs.goety.recipe.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;

public class RitualRecipeJS extends KubeRecipe {

    @Override
    public void serialize() {
        super.serialize();
        if (json == null) {
            return;
        }
        moveToObject("entity_to_sacrifice", "tag", "entity_to_sacrifice.tag");
        moveToObject("entity_to_sacrifice", "display_name", "entity_to_sacrifice.display_name");
        moveToObject("entity_to_convert", "tag", "entity_to_convert.tag");
        moveToObject("entity_to_convert", "display_name", "entity_to_convert.display_name");
    }

    private void moveToObject(String objectKey, String fieldKey, String flatKey) {
        if (json.has(flatKey)) {
            JsonElement val = json.get(flatKey);
            json.remove(flatKey);
            if (val.isJsonPrimitive() && val.getAsString().isEmpty()) {
                return;
            }
            JsonObject obj = json.has(objectKey) && json.get(objectKey).isJsonObject()
                    ? json.getAsJsonObject(objectKey)
                    : new JsonObject();
            obj.add(fieldKey, val);
            json.add(objectKey, obj);
        }
    }
}
