package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.CauldronRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.SizedIngredientComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

public interface CauldronSchema {
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.outputKey("result");
    RecipeKey<List<SizedIngredient>> INGREDIENTS = SizedIngredientComponent.FLAT.instance().asList().inputKey("ingredients");
    RecipeKey<SizedIngredient> TAKE_WITH = SizedIngredientComponent.OPTIONAL_FLAT.inputKey("take_with")
            .optional(new SizedIngredient(Ingredient.EMPTY, 1))
            .alt("takeWith")
            .functionNames("takeWith");
    RecipeKey<Integer> LEVEL_LEFT = NumberComponent.INT.otherKey("levelLeft")
            .optional(3).alwaysWrite()
            .alt("level_left")
            .functionNames("levelLeft");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.otherKey("soulCost")
            .optional(0).alwaysWrite()
            .alt("soul_cost")
            .functionNames("soulCost");
    RecipeKey<Integer> COLOR = NumberComponent.INT.otherKey("color")
            .optional(4159204).alwaysWrite()
            .functionNames("color");

    RecipeSchema SCHEMA = new RecipeSchema(RESULT, INGREDIENTS, TAKE_WITH, LEVEL_LEFT, SOUL_COST, COLOR)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "cauldron"), CauldronRecipeJS.class, CauldronRecipeJS::new))
            .constructor(RESULT, INGREDIENTS);
}
