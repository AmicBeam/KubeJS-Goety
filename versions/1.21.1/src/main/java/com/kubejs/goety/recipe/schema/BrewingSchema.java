package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.BrewingRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

public interface BrewingSchema {
    RecipeKey<Ingredient> INGREDIENT = IngredientComponent.INGREDIENT.inputKey("ingredient");
    RecipeKey<String> EFFECT = StringComponent.ID.otherKey("effect").functionNames("effect");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.otherKey("soulCost").optional(0).alwaysWrite().alt("soul_cost").functionNames("soulCost");
    RecipeKey<Integer> CAPACITY_EXTRA = NumberComponent.INT.otherKey("capacityExtra").optional(0).alwaysWrite().alt("capacity_extra").functionNames("capacityExtra");
    RecipeKey<Integer> DURATION = NumberComponent.INT.otherKey("duration").optional(3600).alwaysWrite().functionNames("duration");

    // 可选字段：entity (entity_type 或 tag)
    RecipeKey<String> ENTITY_TYPE = StringComponent.OPTIONAL_STRING.otherKey("entity.entity_type").optional("").alt("entityType").functionNames("entityType");
    RecipeKey<String> ENTITY_TAG = StringComponent.OPTIONAL_STRING.otherKey("entity.tag").optional("").alt("entityTag").functionNames("entityTag");

    RecipeSchema SCHEMA = new RecipeSchema(
            INGREDIENT, EFFECT, SOUL_COST, CAPACITY_EXTRA, DURATION, ENTITY_TYPE, ENTITY_TAG)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "brewing"), BrewingRecipeJS.class, BrewingRecipeJS::new))
            .constructor(INGREDIENT, EFFECT);
}
