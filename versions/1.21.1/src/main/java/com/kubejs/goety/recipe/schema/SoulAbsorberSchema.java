package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.SoulAbsorberRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

public interface SoulAbsorberSchema {
    RecipeKey<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.inputKey("ingredient");
    RecipeKey<Integer> SOUL_INCREASE = NumberComponent.INT.otherKey("soulIncrease").optional(25).alwaysWrite().alt("soul_increase").functionNames("soulIncrease");
    RecipeKey<Integer> COOKING_TIME = NumberComponent.INT.otherKey("cookingtime").optional(200).alwaysWrite().alt("cookingTime").functionNames("cookingTime");

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENT, SOUL_INCREASE, COOKING_TIME)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "soul_absorber_recipes"), SoulAbsorberRecipeJS.class, SoulAbsorberRecipeJS::new))
            .constructor(INGREDIENT);
}
