package com.kubejs.goety.recipe.schema;

import com.kubejs.goety.recipe.js.RitualRecipeJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface RitualSchema {
    // 必需字段（必须在可选字段之前）
    RecipeKey<OutputItem> RESULT = ItemComponents.OUTPUT.key("result");
    RecipeKey<String> RITUAL_TYPE = StringComponent.ID.key("ritual_type").preferred("ritualType");
    RecipeKey<InputItem[]> INGREDIENTS = ItemComponents.INPUT.asArray().key("ingredients");
    
    // 可选字段
    RecipeKey<InputItem> ACTIVATION_ITEM = ItemComponents.INPUT.key("activation_item").optional(InputItem.EMPTY).preferred("activationItem");
    RecipeKey<String> CRAFT_TYPE = StringComponent.NON_EMPTY.key("craftType").optional("craft");
    RecipeKey<Integer> SOUL_COST = NumberComponent.INT.key("soulCost").optional(0).preferred("soulCost");
    RecipeKey<Integer> DURATION = NumberComponent.INT.key("duration").optional(30).preferred("duration");
    
    // 更多可选字段
    RecipeKey<String> ENTITY_TO_SUMMON = StringComponent.ID.key("entity_to_summon").preferred("entityToSummon").optional("");
    RecipeKey<Integer> SUMMON_LIFE = NumberComponent.INT.key("summonLife").preferred("summonLife").optional(-1);
    RecipeKey<String> ENTITY_TO_SACRIFICE_TAG = StringComponent.ID.key("entity_to_sacrifice.tag").preferred("entityToSacrificeTag").optional("");
    RecipeKey<String> ENTITY_TO_SACRIFICE_DISPLAY_NAME = StringComponent.NON_EMPTY.key("entity_to_sacrifice.display_name").preferred("entityToSacrificeDisplayName").optional("");
    RecipeKey<String> ENTITY_TO_CONVERT_TAG = StringComponent.ID.key("entity_to_convert.tag").preferred("entityToConvertTag").optional("");
    RecipeKey<String> ENTITY_TO_CONVERT_DISPLAY_NAME = StringComponent.NON_EMPTY.key("entity_to_convert.display_name").preferred("entityToConvertDisplayName").optional("");
    RecipeKey<String> ENTITY_TO_CONVERT_INTO = StringComponent.ID.key("entity_to_convert_into").preferred("entityToConvertInto").optional("");
    RecipeKey<String> ENCHANTMENT = StringComponent.ID.key("enchantment").preferred("enchantment").optional("");
    RecipeKey<Integer> XP_LEVEL_COST = NumberComponent.INT.key("xpLevelCost").preferred("xpLevelCost").optional(0);
    RecipeKey<String> RESEARCH = StringComponent.NON_EMPTY.key("research").preferred("research").optional("");

    RecipeSchema SCHEMA = new RecipeSchema(RitualRecipeJS.class, RitualRecipeJS::new,
            // 必需字段在前
            RESULT, RITUAL_TYPE, INGREDIENTS,
            // 可选字段在后
            ACTIVATION_ITEM, CRAFT_TYPE, SOUL_COST, DURATION,
            ENTITY_TO_SUMMON, SUMMON_LIFE, ENTITY_TO_SACRIFICE_TAG, ENTITY_TO_SACRIFICE_DISPLAY_NAME,
            ENTITY_TO_CONVERT_TAG, ENTITY_TO_CONVERT_DISPLAY_NAME, ENTITY_TO_CONVERT_INTO,
            ENCHANTMENT, XP_LEVEL_COST, RESEARCH)
            .constructor(RESULT, RITUAL_TYPE, INGREDIENTS);
}

