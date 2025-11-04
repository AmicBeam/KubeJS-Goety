package com.kubejs.goety;

import com.kubejs.goety.util.EventHandlers;
import dev.latvian.mods.kubejs.bindings.event.ServerEvents;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * KubeJS Goety 模组主类
 * 
 * 这个模组提供了 KubeJS 与 Goety 之间的集成，
 * 允许用户通过 JavaScript 脚本自定义 Goety 仪式的构建条件。
 */
@Mod(KubeJSGoety.MOD_ID)
public class KubeJSGoety {
    
    public static final String MOD_ID = "kubejs_goety";
    
    public KubeJSGoety(IEventBus modEventBus, ModContainer modContainer) {
        // 初始化事件处理器
        EventHandlers.init();
        
        // 监听服务器加载事件，触发我们的自定义事件
        ServerEvents.LOADED.listen(ScriptType.SERVER, event -> {
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
        });
    }
}

