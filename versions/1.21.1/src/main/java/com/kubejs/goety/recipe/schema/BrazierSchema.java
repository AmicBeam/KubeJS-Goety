package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.BrazierRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface BrazierSchema {
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.outputKey("result");
    RecipeKey<List<Ingredient>> INGREDIENTS = IngredientComponent.INGREDIENT.instance().asList().inputKey("ingredients");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.otherKey("soulCost").optional(0).alwaysWrite().alt("soul_cost").functionNames("soulCost");

    RecipeSchema SCHEMA = new RecipeSchema(RESULT, INGREDIENTS, SOUL_COST)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "brazier"), BrazierRecipeJS.class, BrazierRecipeJS::new))
            .constructor(RESULT, INGREDIENTS);
}
