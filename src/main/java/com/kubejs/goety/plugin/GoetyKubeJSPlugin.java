package com.kubejs.goety.plugin;

import com.kubejs.goety.recipe.schema.BrazierSchema;
import com.kubejs.goety.recipe.schema.BrewingSchema;
import com.kubejs.goety.recipe.schema.CauldronSchema;
import com.kubejs.goety.recipe.schema.CursedInfuserSchema;
import com.kubejs.goety.recipe.schema.PulverizeSchema;
import com.kubejs.goety.recipe.schema.RitualSchema;
import com.kubejs.goety.recipe.schema.SoulAbsorberSchema;
import com.kubejs.goety.util.EventHandlers;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/**
 * KubeJS Goety 插件
 * 允许 KubeJS 脚本访问 Goety 的仪式 API 和配方系统
 */
public class GoetyKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void init() {
        // 注册自定义物品类型 'kubejs_goety:scroll'：
        // 脚本可用 event.create(id, 'kubejs_goety:scroll').research('xxx') 创建真正的
        // Goety Scroll 物品（原生右键学习 + tooltip + JEI 仪式页显示），
        // 必须早于脚本加载注册，故放在 init()
        RegistryInfo.ITEM.addType("kubejs_goety:scroll", com.kubejs.goety.item.ScrollItemBuilder.class,
                com.kubejs.goety.item.ScrollItemBuilder::new);
    }

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

        // 允许访问 Goety 的研究系统（Research / ResearchList）
        filter.allow("com.Polarice3.Goety.common.research");

        // 允许访问 Goety 的灵魂能量工具类（SEHelper，用于研究查询/授予）
        filter.allow("com.Polarice3.Goety.utils");
    }
    
    @Override
    public void registerEvents() {
        // 注册 GoetyEvents 事件组
        EventHandlers.GoetyEvents.register();
    }
    
    @Override
    public void registerBindings(dev.latvian.mods.kubejs.script.BindingsEvent event) {
        // 暴露研究辅助工具为全局绑定（STARTUP 与 SERVER 均需）：
        // - startup 脚本：顶层注册研究（早于物品创建/读档，保证 JEI 引用比较与存档反查）
        // - server 脚本：hasResearch/grantResearch 等运行时查询
        // 脚本中可直接使用 goetyResearch.registerResearch(id) / hasResearch(player, id) 等
        event.add("goetyResearch", com.kubejs.goety.util.ResearchHelper.class);

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
        } else if (event.getType() == ScriptType.CLIENT) {
            // 为脚本注册的自定义卷轴自动添加默认语言键。
            // 每个研究都会自动注册 info.goety.research.<id>（右键习得消息）与
            // info.goety.items.<id>（tooltip 介绍）。ClientEvents.lang 带 REQUIRES_STRING，
            // 监听时必须指定语言代码（listenJava 的 id 参数），此处按语言分别注册中/英默认文案，
            // 脚本用户无需自行写任何 lang；玩家在自己的 lang 文件中写同名键即可覆盖。
            dev.latvian.mods.kubejs.bindings.event.ClientEvents.LANG.listenJava(ScriptType.CLIENT, "zh_cn", e -> {
                addDefaultScrollLangs((dev.latvian.mods.kubejs.client.LangEventJS) e,
                        com.kubejs.goety.item.KubeJSScroll.DEFAULT_LEARN_ZH,
                        com.kubejs.goety.item.KubeJSScroll.DEFAULT_DESC_ZH);
                return null;
            });
            dev.latvian.mods.kubejs.bindings.event.ClientEvents.LANG.listenJava(ScriptType.CLIENT, "en_us", e -> {
                addDefaultScrollLangs((dev.latvian.mods.kubejs.client.LangEventJS) e,
                        com.kubejs.goety.item.KubeJSScroll.DEFAULT_LEARN_EN,
                        com.kubejs.goety.item.KubeJSScroll.DEFAULT_DESC_EN);
                return null;
            });
        }
    }

    /** 为所有已注册的自定义卷轴研究添加默认语言键（右键消息 + tooltip 介绍） */
    private static void addDefaultScrollLangs(dev.latvian.mods.kubejs.client.LangEventJS langEvent,
                                              String learnText, String descText) {
        for (String id : com.kubejs.goety.item.ScrollItemBuilder.getTrackedResearchIds()) {
            langEvent.add("info.goety.research." + id, learnText);
            langEvent.add("info.goety.items." + id, descText);
        }
    }
    
    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        // 注册 Goety 的配方类型
        event.namespace("goety")
                .register("ritual", RitualSchema.SCHEMA)
                .register("brewing", BrewingSchema.SCHEMA)
                .register("cauldron", CauldronSchema.SCHEMA)
                .register("pulverize", PulverizeSchema.SCHEMA)
                .register("cursed_infuser_recipes", CursedInfuserSchema.SCHEMA)
                .register("brazier", BrazierSchema.SCHEMA)
                .register("soul_absorber_recipes", SoulAbsorberSchema.SCHEMA);
    }
}
