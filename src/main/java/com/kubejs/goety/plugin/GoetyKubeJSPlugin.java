package com.kubejs.goety.plugin;

import com.kubejs.goety.recipe.schema.BrazierSchema;
import com.kubejs.goety.recipe.schema.BrewingSchema;
import com.kubejs.goety.recipe.schema.CursedInfuserSchema;
import com.kubejs.goety.recipe.schema.PulverizeSchema;
import com.kubejs.goety.recipe.schema.RitualSchema;
import com.kubejs.goety.recipe.schema.SoulAbsorberSchema;
import com.kubejs.goety.util.EventHandlers;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/**
 * KubeJS Goety 插件
 * 允许 KubeJS 脚本访问 Goety 的仪式 API 和配方系统
 */
public class GoetyKubeJSPlugin extends KubeJSPlugin {
    
    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
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
    public void registerEvents() {
        // 注册 GoetyEvents 事件组
        EventHandlers.GoetyEvents.register();
    }
    
    @Override
    public void registerBindings(dev.latvian.mods.kubejs.script.BindingsEvent event) {
        // 在 SERVER 脚本类型的 registerBindings 阶段注册事件监听器
        // 此时 ScriptManager 已经初始化，可以安全调用
        if (event.getType() == ScriptType.SERVER) {
            dev.latvian.mods.kubejs.bindings.event.ServerEvents.LOADED.listenJava(ScriptType.SERVER, null, e -> {
                // 触发注册仪式事件
                if (EventHandlers.registerRitual.hasListeners()) {
                    EventHandlers.registerRitual.post(new com.kubejs.goety.event.RegisterRitualEventJS());
                }
                
                // 触发修改仪式事件
                if (EventHandlers.modifyRitual.hasListeners()) {
                    EventHandlers.modifyRitual.post(new com.kubejs.goety.event.ModifyRitualEventJS());
                }
                
                // 触发删除仪式事件
                if (EventHandlers.removeRitual.hasListeners()) {
                    EventHandlers.removeRitual.post(new com.kubejs.goety.event.RemoveRitualEventJS());
                }
                
                // 触发注册药酿事件
                if (EventHandlers.registerBrew.hasListeners()) {
                    EventHandlers.registerBrew.post(new com.kubejs.goety.event.RegisterBrewEventJS());
                }
                
                return null;
            });
        }
    }
    
    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        // 注册 Goety 的配方类型
        event.namespace("goety")
                .register("ritual", RitualSchema.SCHEMA)
                .register("brewing", BrewingSchema.SCHEMA)
                .register("pulverize", PulverizeSchema.SCHEMA)
                .register("cursed_infuser_recipes", CursedInfuserSchema.SCHEMA)
                .register("brazier", BrazierSchema.SCHEMA)
                .register("soul_absorber_recipes", SoulAbsorberSchema.SCHEMA);
    }
}

