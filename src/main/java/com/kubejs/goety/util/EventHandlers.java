package com.kubejs.goety.util;

import com.kubejs.goety.event.RegisterRitualEventJS;
import com.kubejs.goety.event.ModifyRitualEventJS;
import com.kubejs.goety.event.RemoveRitualEventJS;
import com.kubejs.goety.event.RegisterBrewEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.typings.Info;

/**
 * Goety 事件处理器
 * 
 * 定义 GoetyEvents 事件组，用于在脚本中处理仪式相关事件
 */
@Info("Goety 事件组，用于在脚本中处理仪式和药酿相关事件")
public class EventHandlers {
    
    /**
     * GoetyEvents 事件组
     * 在脚本中使用：GoetyEvents.registerRitual(), GoetyEvents.modifyRitual(), GoetyEvents.removeRitual(), GoetyEvents.registerBrew()
     */
    public static final EventGroup GoetyEvents = EventGroup.of("GoetyEvents");
    
    /**
     * 注册新仪式的事件
     * 在 server_scripts 中使用：GoetyEvents.registerRitual(event => { ... })
     */
    public static final EventHandler registerRitual = GoetyEvents.server("registerRitual", () -> RegisterRitualEventJS.class);
    
    /**
     * 修改现有仪式条件的事件
     * 在 server_scripts 中使用：GoetyEvents.modifyRitual(event => { ... })
     */
    public static final EventHandler modifyRitual = GoetyEvents.server("modifyRitual", () -> ModifyRitualEventJS.class);
    
    /**
     * 删除仪式类型的事件
     * 在 server_scripts 中使用：GoetyEvents.removeRitual(event => { ... })
     */
    public static final EventHandler removeRitual = GoetyEvents.server("removeRitual", () -> RemoveRitualEventJS.class);
    
    /**
     * 注册药酿配置的事件
     * 在 server_scripts 中使用：GoetyEvents.registerBrew(event => { ... })
     */
    public static final EventHandler registerBrew = GoetyEvents.server("registerBrew", () -> RegisterBrewEventJS.class);
    
    /**
     * 初始化事件处理器
     */
    public static void init() {
        // 事件在服务器加载时自动触发
        // 通过 ServerEvents.LOADED 事件来触发我们的自定义事件
    }
}

