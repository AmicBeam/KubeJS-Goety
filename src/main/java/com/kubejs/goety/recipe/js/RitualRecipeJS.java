package com.kubejs.goety.recipe.js;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.core.ItemStackKJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RitualRecipeJS extends RecipeJS {
    
    /**
     * 重写 writeInputItem 方法，确保带 NBT 的 Ingredient 正确序列化为 forge:nbt 格式
     * 这样 Goety 的 RitualRecipe 才能正确解析 NBT 数据
     */
    @Override
    public JsonElement writeInputItem(InputItem value) {
        Ingredient ingredient = value.ingredient;
        JsonElement defaultJson = ingredient.toJson();
        
        // 检查是否已经是 forge:nbt 格式
        if (defaultJson.isJsonObject()) {
            JsonObject obj = defaultJson.getAsJsonObject();
            if (obj.has("type") && "forge:nbt".equals(obj.get("type").getAsString())) {
                if (value.count > 1 && !obj.has("count")) {
                    obj.addProperty("count", value.count);
                }
                return obj;
            }
        }
        
        // 检查是否有带 NBT 的单个物品栈（Item.of() 创建的带 NBT 物品）
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
        
        // 默认情况：处理 count
        if (value.count > 1) {
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("count", value.count);
            wrapper.add("ingredient", defaultJson);
            return wrapper;
        }
        
        return defaultJson;
    }
}

