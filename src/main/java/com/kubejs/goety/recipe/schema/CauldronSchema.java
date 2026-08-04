package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.CauldronRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface CauldronSchema {
    RecipeKey<OutputItem> RESULT = ItemComponents.OUTPUT.key("result");
    RecipeKey<InputItem[]> INGREDIENTS = ItemComponents.INPUT.asArray().key("ingredients");
    RecipeKey<InputItem> TAKE_WITH = ItemComponents.INPUT.key("take_with")
            .optional(InputItem.EMPTY)
            .preferred("takeWith");
    RecipeKey<Integer> LEVEL_LEFT = NumberComponent.INT.key("levelLeft")
            .optional(3)
            .preferred("levelLeft");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.key("soulCost")
            .optional(0)
            .preferred("soulCost");
    RecipeKey<Integer> COLOR = NumberComponent.INT.key("color")
            .optional(4159204)
            .preferred("color");

    RecipeSchema SCHEMA = new RecipeSchema(CauldronRecipeJS.class, CauldronRecipeJS::new,
            RESULT, INGREDIENTS, TAKE_WITH, LEVEL_LEFT, SOUL_COST, COLOR)
            .constructor(RESULT, INGREDIENTS);
}
