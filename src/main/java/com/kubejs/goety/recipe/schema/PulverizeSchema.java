package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.PulverizeRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface PulverizeSchema {
    RecipeKey<InputItem> INGREDIENT = ItemComponents.INPUT.key("ingredient");
    RecipeKey<String> BLOCK_RESULT = StringComponent.ID.key("block_result").preferred("blockResult").optional("");
    RecipeKey<OutputItem> ITEM_RESULT = ItemComponents.OUTPUT.key("item_result").preferred("itemResult").optional(OutputItem.EMPTY);

    RecipeSchema SCHEMA = new RecipeSchema(PulverizeRecipeJS.class, PulverizeRecipeJS::new,
            INGREDIENT, BLOCK_RESULT, ITEM_RESULT)
            .constructor(INGREDIENT);
}

