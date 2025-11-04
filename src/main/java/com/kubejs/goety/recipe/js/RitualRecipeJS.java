package com.kubejs.goety.recipe.js;

import com.kubejs.goety.recipe.schema.RitualSchema;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.KubeRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.util.ListJS;

import java.util.List;

public class RitualRecipeJS extends KubeRecipe {
    public RitualRecipeJS(RecipeKey[] keys, Object[] values) {
        super(keys, values);
    }

    @Override
    public String getGroup() {
        return getValue(RitualSchema.CRAFT_TYPE);
    }

    @Override
    public JsonObject serializeJson() {
        var json = super.serializeJson();
        
        // 添加必需的字段
        json.addProperty("ritual_type", getValue(RitualSchema.RITUAL_TYPE));
        json.addProperty("craftType", getValue(RitualSchema.CRAFT_TYPE));
        json.addProperty("soulCost", getValue(RitualSchema.SOUL_COST));
        json.addProperty("duration", getValue(RitualSchema.DURATION));
        
        // 添加 activation_item
        var activationItem = getValue(RitualSchema.ACTIVATION_ITEM);
        if (activationItem != null && !activationItem.isEmpty()) {
            json.add("activation_item", activationItem.toJson());
        }
        
        // 添加 ingredients 数组
        var ingredients = getValue(RitualSchema.INGREDIENTS);
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
        
        // 添加 result
        json.add("result", getValue(RitualSchema.RESULT).toJson());
        
        // 添加可选字段
        var entityToSummon = getValue(RitualSchema.ENTITY_TO_SUMMON);
        if (entityToSummon != null && !entityToSummon.isEmpty()) {
            json.addProperty("entity_to_summon", entityToSummon);
        }
        
        var summonLife = getValue(RitualSchema.SUMMON_LIFE);
        if (summonLife != null && summonLife != -1) {
            json.addProperty("summonLife", summonLife);
        }
        
        var entityToSacrificeTag = getValue(RitualSchema.ENTITY_TO_SACRIFICE_TAG);
        if (entityToSacrificeTag != null && !entityToSacrificeTag.isEmpty()) {
            var sacrificeObj = new JsonObject();
            sacrificeObj.addProperty("tag", entityToSacrificeTag);
            var displayName = getValue(RitualSchema.ENTITY_TO_SACRIFICE_DISPLAY_NAME);
            if (displayName != null && !displayName.isEmpty()) {
                sacrificeObj.addProperty("display_name", displayName);
            }
            json.add("entity_to_sacrifice", sacrificeObj);
        }
        
        var entityToConvertTag = getValue(RitualSchema.ENTITY_TO_CONVERT_TAG);
        if (entityToConvertTag != null && !entityToConvertTag.isEmpty()) {
            var convertObj = new JsonObject();
            convertObj.addProperty("tag", entityToConvertTag);
            var displayName = getValue(RitualSchema.ENTITY_TO_CONVERT_DISPLAY_NAME);
            if (displayName != null && !displayName.isEmpty()) {
                convertObj.addProperty("display_name", displayName);
            }
            json.add("entity_to_convert", convertObj);
        }
        
        var entityToConvertInto = getValue(RitualSchema.ENTITY_TO_CONVERT_INTO);
        if (entityToConvertInto != null && !entityToConvertInto.isEmpty()) {
            json.addProperty("entity_to_convert_into", entityToConvertInto);
        }
        
        var enchantment = getValue(RitualSchema.ENCHANTMENT);
        if (enchantment != null && !enchantment.isEmpty()) {
            json.addProperty("enchantment", enchantment);
            var xpLevelCost = getValue(RitualSchema.XP_LEVEL_COST);
            if (xpLevelCost != null && ((Integer) xpLevelCost) > 0) {
                json.addProperty("xpLevelCost", xpLevelCost);
            }
        }
        
        var research = getValue(RitualSchema.RESEARCH);
        if (research != null && !research.isEmpty()) {
            json.addProperty("research", research);
        }
        
        return json;
    }
}

