package com.kubejs.goety.plugin;

import com.kubejs.goety.recipe.schema.BrazierSchema;
import com.kubejs.goety.recipe.schema.BrewingSchema;
import com.kubejs.goety.recipe.schema.CauldronSchema;
import com.kubejs.goety.recipe.schema.CursedInfuserSchema;
import com.kubejs.goety.recipe.schema.PulverizeSchema;
import com.kubejs.goety.recipe.schema.RitualSchema;
import com.kubejs.goety.recipe.schema.SoulAbsorberSchema;
import com.kubejs.goety.util.EventHandlers;
import com.kubejs.goety.brew.BrewData;
import com.kubejs.goety.research.GoetyResearchScriptAPI;
import com.kubejs.goety.research.CustomResearchScrollItem;
import com.kubejs.goety.research.ResearchData;
import com.kubejs.goety.research.ResearchNetwork;
import dev.latvian.mods.kubejs.core.RecipeManagerKJS;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.builtin.event.ServerEvents;
import dev.latvian.mods.kubejs.recipe.RecipesKubeEvent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.KubeJSContext;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * KubeJS Goety 插件
 * 允许 KubeJS 脚本访问 Goety 的仪式 API 和配方系统
 */
public class GoetyKubeJSPlugin implements KubeJSPlugin {

    private static volatile KubeJSContext serverContext;
    private static volatile Scriptable serverScope;

    public static Context getServerContext() {
        return serverContext;
    }

    public static Scriptable getServerScope() {
        return serverScope != null ? serverScope : (serverContext != null ? serverContext.topLevelScope : null);
    }

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.of(Registries.ITEM, c -> {
            c.add(ResourceLocation.fromNamespaceAndPath("kubejs", "goety_research_scroll"), CustomResearchScrollItem.Builder.class,
                    CustomResearchScrollItem.Builder::new);
        });
    }

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
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type() == ScriptType.SERVER) {
            serverContext = bindings.context();
            serverScope = bindings.scope();
            bindings.add("GoetyResearch", GoetyResearchScriptAPI.INSTANCE);
            ServerEvents.LOADED.listenJava(ScriptType.SERVER, null, event -> {
                postServerRegistrations();
                return null;
            });
        }
    }

    @Override
    public void beforeRecipeLoading(RecipesKubeEvent event, RecipeManagerKJS recipeManager, java.util.Map<net.minecraft.resources.ResourceLocation, com.google.gson.JsonElement> jsonMap) {
        ResearchData.beginRegistration();
        if (EventHandlers.registerResearch.hasListeners()) {
            EventHandlers.registerResearch.post(new com.kubejs.goety.event.RegisterResearchEventJS());
        }
        ResearchData.validateDefinitions();
        ResearchNetwork.syncAllPlayers();
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        var server = ServerLifecycleHooks.getCurrentServer();
        // Initial registration keeps the 1.20.1 loaded-event timing. A live
        // server reload can apply the new definitions as soon as scripts load.
        if (manager.scriptType == ScriptType.SERVER && server != null && server.isRunning()) {
            postServerRegistrations();
        }
    }

    private static void postServerRegistrations() {
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
        // A newly loaded world only owns the level tables its scripts configure now.
        BrewData.resetScriptedLevelTables();
        if (EventHandlers.registerBrew.hasListeners()) {
            EventHandlers.registerBrew.post(new com.kubejs.goety.event.RegisterBrewEventJS());
        }
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegistry registry) {
        // 注册 Goety 的配方类型
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "ritual"), RitualSchema.SCHEMA);
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "brewing"), BrewingSchema.SCHEMA);
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "cauldron"), CauldronSchema.SCHEMA);
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "pulverize"), PulverizeSchema.SCHEMA);
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "cursed_infuser_recipes"), CursedInfuserSchema.SCHEMA);
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "brazier"), BrazierSchema.SCHEMA);
        registry.register(ResourceLocation.fromNamespaceAndPath("goety", "soul_absorber_recipes"), SoulAbsorberSchema.SCHEMA);
    }

    // KubeJS also clears general caches after /reload. These references belong
    // to live callbacks and remain valid until new bindings replace them.
    public static void clearServerContext() {
        serverContext = null;
        serverScope = null;
    }
}
