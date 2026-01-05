package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.CursedInfuserRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface CursedInfuserSchema {
    RecipeKey<InputItem> INGREDIENT = ItemComponents.INPUT.key("ingredient");
    RecipeKey<OutputItem> RESULT = ItemComponents.OUTPUT.key("result");
    RecipeKey<Integer> COOKING_TIME = NumberComponent.INT.key("cookingTime").optional(60).preferred("cookingTime");
    RecipeKey<Boolean> GRIM = BooleanComponent.BOOLEAN.key("grim").optional(false).preferred("grim");

    RecipeSchema SCHEMA = new RecipeSchema(CursedInfuserRecipeJS.class, CursedInfuserRecipeJS::new,
            INGREDIENT, RESULT, COOKING_TIME, GRIM)
            .constructor(RESULT, INGREDIENT);
}
