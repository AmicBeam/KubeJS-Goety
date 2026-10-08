package com.kubejs.goety.recipe.js;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;

public class CauldronRecipeJS extends KubeRecipe {

    @Override
    public void serialize() {
        super.serialize();
        if (json == null) {
            return;
        }

        // Goety 3.1.4's nullable codec default cannot decode an omitted id.
        json.addProperty("id", getOrCreateId().toString());

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
                JsonElement ingredient = counted.has("ingredient") ? counted.get("ingredient") : getIngredientWithoutCount(counted);
                for (int i = 0; i < count; i++) {
                    expanded.add(ingredient.deepCopy());
                }
            } else {
                expanded.add(element);
            }
        }
        json.add("ingredients", expanded);
    }

    private JsonElement getIngredientWithoutCount(JsonObject obj) {
        JsonObject copy = obj.deepCopy();
        copy.remove("count");
        return copy;
    }

    private void normalizeTakeWith() {
        if (!json.has("take_with")) {
            json.add("take_with", new JsonArray());
            return;
        }

        JsonElement takeWith = json.get("take_with");
        if (isCountedIngredient(takeWith)) {
            JsonObject obj = takeWith.getAsJsonObject();
            JsonElement inner = obj.has("ingredient") ? obj.get("ingredient") : getIngredientWithoutCount(obj);
            if (isEmptyIngredient(inner)) {
                json.add("take_with", new JsonArray());
            } else {
                json.add("take_with", inner);
            }
        } else if (isEmptyIngredient(takeWith)) {
            json.add("take_with", new JsonArray());
        }
    }

    private boolean isEmptyIngredient(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return true;
        }
        if (element.isJsonArray() && element.getAsJsonArray().isEmpty()) {
            return true;
        }
        if (element.isJsonObject() && element.getAsJsonObject().entrySet().isEmpty()) {
            return true;
        }
        return false;
    }

    private boolean isCountedIngredient(JsonElement element) {
        return element != null
                && element.isJsonObject()
                && element.getAsJsonObject().has("count")
                && (element.getAsJsonObject().has("ingredient") || element.getAsJsonObject().has("item") || element.getAsJsonObject().has("tag"));
    }
}
