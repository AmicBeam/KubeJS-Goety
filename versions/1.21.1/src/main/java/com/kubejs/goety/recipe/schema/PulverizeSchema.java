package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.PulverizeRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface PulverizeSchema {
    RecipeKey<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.inputKey("ingredient");
    RecipeKey<String> BLOCK_RESULT = StringComponent.OPTIONAL_STRING.otherKey("block_result").optional("").alt("blockResult").functionNames("blockResult");
    RecipeKey<ItemStack> ITEM_RESULT = ItemStackComponent.OPTIONAL_ITEM_STACK.outputKey("item_result").optional(ItemStack.EMPTY).alt("itemResult").functionNames("itemResult");

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENT, BLOCK_RESULT, ITEM_RESULT)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "pulverize"), PulverizeRecipeJS.class, PulverizeRecipeJS::new))
            .constructor(INGREDIENT);
}
