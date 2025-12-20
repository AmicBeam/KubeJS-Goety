package com.kubejs.goety.recipe.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.core.ItemStackKJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class SoulAbsorberRecipeJS extends RecipeJS {
    
    @Override
    public JsonElement writeInputItem(InputItem value) {
        Ingredient ingredient = value.ingredient;
        JsonElement defaultJson = ingredient.toJson();
        
        if (defaultJson.isJsonObject()) {
            JsonObject obj = defaultJson.getAsJsonObject();
            if (obj.has("type") && "forge:nbt".equals(obj.get("type").getAsString())) {
                if (value.count > 1 && !obj.has("count")) {
                    obj.addProperty("count", value.count);
                }
                return obj;
            }
        }
        
        ItemStack[] items = ingredient.getItems();
        if (items.length == 1 && items[0].hasTag()) {
            ItemStack stack = items[0];
            ItemStackKJS stackKJS = (ItemStackKJS) (Object) stack;
            
            JsonObject json = new JsonObject();
            json.addProperty("type", "forge:nbt");
            json.addProperty("item", stackKJS.kjs$getId());
            json.addProperty("nbt", stack.getTag().toString());
            if (value.count > 1) {
                json.addProperty("count", value.count);
            }
            return json;
        }
        
        if (value.count > 1) {
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("count", value.count);
            wrapper.add("ingredient", defaultJson);
            return wrapper;
        }
        
        return defaultJson;
    }
}

