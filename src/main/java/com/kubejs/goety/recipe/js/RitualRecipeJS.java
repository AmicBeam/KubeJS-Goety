package com.kubejs.goety.recipe.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;

public class RitualRecipeJS extends RecipeJS {
    
    /**
     * 仪式配方输入遵循 KubeJS 默认匹配行为：
     * 普通输入默认忽略 NBT，脚本可显式使用 weakNBT()/strongNBT() 控制匹配方式。
     */
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
        moveToObject("entity_to_sacrifice", "tag", "entity_to_sacrifice.tag");
        moveToObject("entity_to_sacrifice", "display_name", "entity_to_sacrifice.display_name");
        moveToObject("entity_to_convert", "tag", "entity_to_convert.tag");
        moveToObject("entity_to_convert", "display_name", "entity_to_convert.display_name");
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
