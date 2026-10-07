package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.RitualRecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface RitualSchema {
    RecipeKey<ItemStack> RESULT = ItemStackComponent.ITEM_STACK.outputKey("result");
    RecipeKey<String> RITUAL_TYPE = StringComponent.ID.otherKey("ritual_type").alt("ritualType").functionNames("ritualType");
    RecipeKey<List<Ingredient>> INGREDIENTS = IngredientComponent.INGREDIENT.instance().asList().inputKey("ingredients");

    // 可选字段
    RecipeKey<Ingredient> ACTIVATION_ITEM = IngredientComponent.OPTIONAL_INGREDIENT.inputKey("activation_item").optional(Ingredient.EMPTY).alwaysWrite().alt("activationItem").functionNames("activationItem");
    RecipeKey<String> CRAFT_TYPE = StringComponent.STRING.otherKey("craftType").optional("craft").alt("craft_type").functionNames("craftType");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.otherKey("soulCost").optional(0).alwaysWrite().alt("soul_cost").functionNames("soulCost");
    RecipeKey<Integer> DURATION = NumberComponent.INT.otherKey("duration").optional(30).alwaysWrite().functionNames("duration");

    // 更多可选字段
    RecipeKey<String> ENTITY_TO_SUMMON = StringComponent.OPTIONAL_STRING.otherKey("entity_to_summon").optional("").alt("entityToSummon").functionNames("entityToSummon");
    RecipeKey<Integer> SUMMON_LIFE = NumberComponent.INT.otherKey("summonLife").optional(-1).alwaysWrite().alt("summon_life").functionNames("summonLife");
    RecipeKey<String> ENTITY_TO_SACRIFICE_TAG = StringComponent.OPTIONAL_STRING.otherKey("entity_to_sacrifice.tag").optional("").alt("entityToSacrificeTag").functionNames("entityToSacrificeTag");
    RecipeKey<String> ENTITY_TO_SACRIFICE_DISPLAY_NAME = StringComponent.OPTIONAL_STRING.otherKey("entity_to_sacrifice.display_name").optional("").alt("entityToSacrificeDisplayName").functionNames("entityToSacrificeDisplayName");
    RecipeKey<String> ENTITY_TO_CONVERT_TAG = StringComponent.OPTIONAL_STRING.otherKey("entity_to_convert.tag").optional("").alt("entityToConvertTag").functionNames("entityToConvertTag");
    RecipeKey<String> ENTITY_TO_CONVERT_DISPLAY_NAME = StringComponent.OPTIONAL_STRING.otherKey("entity_to_convert.display_name").optional("").alt("entityToConvertDisplayName").functionNames("entityToConvertDisplayName");
    RecipeKey<String> ENTITY_TO_CONVERT_INTO = StringComponent.OPTIONAL_STRING.otherKey("entity_to_convert_into").optional("").alt("entityToConvertInto").functionNames("entityToConvertInto");
    RecipeKey<String> ENCHANTMENT = StringComponent.OPTIONAL_STRING.otherKey("enchantment").optional("").functionNames("enchantment");
    RecipeKey<Integer> XP_LEVEL_COST = NumberComponent.INT.otherKey("xpLevelCost").optional(0).alwaysWrite().alt("xp_level_cost").functionNames("xpLevelCost");
    RecipeKey<String> RESEARCH = StringComponent.OPTIONAL_STRING.otherKey("research").optional("").functionNames("research");

    RecipeSchema SCHEMA = new RecipeSchema(
            RESULT, RITUAL_TYPE, INGREDIENTS,
            ACTIVATION_ITEM, CRAFT_TYPE, SOUL_COST, DURATION,
            ENTITY_TO_SUMMON, SUMMON_LIFE, ENTITY_TO_SACRIFICE_TAG, ENTITY_TO_SACRIFICE_DISPLAY_NAME,
            ENTITY_TO_CONVERT_TAG, ENTITY_TO_CONVERT_DISPLAY_NAME, ENTITY_TO_CONVERT_INTO,
            ENCHANTMENT, XP_LEVEL_COST, RESEARCH)
            .factory(new KubeRecipeFactory(ResourceLocation.fromNamespaceAndPath("kubejs_goety", "ritual"), RitualRecipeJS.class, RitualRecipeJS::new))
            .constructor(RESULT, RITUAL_TYPE, INGREDIENTS);
}
