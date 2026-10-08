package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.CursedInfuserRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface CursedInfuserSchema {
    RecipeKey<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.inputKey("ingredient");
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.outputKey("result");
    RecipeKey<Integer> COOKING_TIME = NumberComponent.INT.otherKey("cookingTime").optional(60).alwaysWrite().alt("cooking_time").functionNames("cookingTime");
    RecipeKey<Boolean> GRIM = BooleanComponent.BOOLEAN.otherKey("grim").optional(false).alwaysWrite().functionNames("grim");

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENT, RESULT, COOKING_TIME, GRIM)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "cursed_infuser_recipes"), CursedInfuserRecipeJS.class, CursedInfuserRecipeJS::new))
            .constructor(RESULT, INGREDIENT);
}
