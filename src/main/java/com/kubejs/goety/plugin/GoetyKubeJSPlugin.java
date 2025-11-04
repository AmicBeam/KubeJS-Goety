package com.kubejs.goety.plugin;

import com.kubejs.goety.recipe.schema.BrazierSchema;
import com.kubejs.goety.recipe.schema.BrewingSchema;
import com.kubejs.goety.recipe.schema.CursedInfuserSchema;
import com.kubejs.goety.recipe.schema.PulverizeSchema;
import com.kubejs.goety.recipe.schema.RitualSchema;
import com.kubejs.goety.recipe.schema.SoulAbsorberSchema;
import com.kubejs.goety.util.EventHandlers;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;

/**
 * KubeJS Goety 插件
 * 允许 KubeJS 脚本访问 Goety 的仪式 API 和配方系统
 */
public class GoetyKubeJSPlugin implements KubeJSPlugin {
    
    @Override
    public void registerClasses(ClassFilter filter) {
        // 允许访问 Goety 的仪式 API
        filter.allow("com.Polarice3.Goety.api.ritual");
        
        // 允许访问 Goety 的方块实体类（用于仪式检查）
        filter.allow("com.Polarice3.Goety.common.blocks.entities");
        
        // 允许访问 Goety 的仪式类型实现（如果需要）
        filter.allow("com.Polarice3.Goety.common.ritual.type");
        
        // 允许访问 Goety 的药酿系统
        filter.allow("com.Polarice3.Goety.common.effects.brew");
        filter.allow("com.Polarice3.Goety.common.effects.brew.modifiers");
    }
    
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        // 注册 GoetyEvents 事件组
        registry.register(EventHandlers.GoetyEvents);
    }
    
    @Override
    public void registerRecipeSchemas(dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry registry) {
        // 注册 Goety 的配方类型
        registry.namespace("goety")
                .register("ritual", RitualSchema.SCHEMA)
                .register("brewing", BrewingSchema.SCHEMA)
                .register("pulverize", PulverizeSchema.SCHEMA)
                .register("cursed_infuser_recipes", CursedInfuserSchema.SCHEMA)
                .register("brazier", BrazierSchema.SCHEMA)
                .register("soul_absorber_recipes", SoulAbsorberSchema.SCHEMA);
    }
}

