package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.BrazierRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface BrazierSchema {
    RecipeKey<OutputItem> RESULT = ItemComponents.OUTPUT.key("result");
    RecipeKey<InputItem[]> INGREDIENTS = ItemComponents.INPUT.asArray().key("ingredients");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.key("soulCost").optional(0).preferred("soulCost");

    RecipeSchema SCHEMA = new RecipeSchema(BrazierRecipeJS.class, BrazierRecipeJS::new,
            RESULT, INGREDIENTS, SOUL_COST)
            .constructor(RESULT, INGREDIENTS);
}

