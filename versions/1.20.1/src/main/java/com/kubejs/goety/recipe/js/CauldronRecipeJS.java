package com.kubejs.goety.recipe.js;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeJS;

public class CauldronRecipeJS extends RecipeJS {
    @Override
    public JsonElement writeInputItem(InputItem value) {
        JsonElement ingredient = value.ingredient.toJson();
        if (value.count <= 1) {
            return ingredient;
        }

        JsonObject counted = new JsonObject();
        counted.addProperty("count", value.count);
        counted.add("ingredient", ingredient);
        return counted;
    }

    @Override
    public void serialize() {
        super.serialize();
        if (json == null) {
            return;
        }

        expandCountedIngredients();
        normalizeTakeWith();
    }

    private void expandCountedIngredients() {
        if (!json.has("ingredients") || !json.get("ingredients").isJsonArray()) {
            return;
        }

        JsonArray expanded = new JsonArray();
        for (JsonElement element : json.getAsJsonArray("ingredients")) {
            if (isCountedIngredient(element)) {
                JsonObject counted = element.getAsJsonObject();
                int count = Math.max(1, counted.get("count").getAsInt());
                JsonElement ingredient = counted.get("ingredient");
                for (int i = 0; i < count; i++) {
                    expanded.add(ingredient.deepCopy());
                }
            } else {
                expanded.add(element);
            }
        }
        json.add("ingredients", expanded);
    }

    private void normalizeTakeWith() {
        if (!json.has("take_with")) {
            json.add("take_with", new JsonArray());
            return;
        }

        JsonElement takeWith = json.get("take_with");
        if (isCountedIngredient(takeWith)) {
            json.add("take_with", takeWith.getAsJsonObject().get("ingredient"));
        }
    }

    private boolean isCountedIngredient(JsonElement element) {
        return element != null
                && element.isJsonObject()
                && element.getAsJsonObject().has("count")
                && element.getAsJsonObject().has("ingredient");
    }
}
