package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.BrewingRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface BrewingSchema {
    RecipeKey<InputItem> INGREDIENT = ItemComponents.INPUT.key("ingredient");
    RecipeKey<String> EFFECT = StringComponent.ID.key("effect").preferred("effect");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.key("soulCost").optional(0).preferred("soulCost");
    RecipeKey<Integer> CAPACITY_EXTRA = NumberComponent.INT.key("capacityExtra").optional(0).preferred("capacityExtra");
    RecipeKey<Integer> DURATION = NumberComponent.INT.key("duration").optional(3600).preferred("duration");
    
    // 可选字段：entity (entity_type 或 tag)
    RecipeKey<String> ENTITY_TYPE = StringComponent.ID.key("entity.entity_type").preferred("entityType").optional("");
    RecipeKey<String> ENTITY_TAG = StringComponent.ID.key("entity.tag").preferred("entityTag").optional("");

    RecipeSchema SCHEMA = new RecipeSchema(BrewingRecipeJS.class, BrewingRecipeJS::new,
            INGREDIENT, EFFECT, SOUL_COST, CAPACITY_EXTRA, DURATION, ENTITY_TYPE, ENTITY_TAG)
            .constructor(INGREDIENT, EFFECT);
}

