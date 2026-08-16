package com.kubejs.goety;

import com.kubejs.goety.research.ResearchGameplayEvents;
import com.kubejs.goety.research.ResearchNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * KubeJS Goety 模组主类
 * 
 * 这个模组提供了 KubeJS 与 Goety 之间的集成，
 * 允许用户通过 JavaScript 脚本自定义 Goety 仪式的构建条件。
 * 
 * 事件注册在 GoetyKubeJSPlugin 中完成。
 */
@Mod(KubeJSGoety.MOD_ID)
public class KubeJSGoety {
    
    public static final String MOD_ID = "kubejs_goety";
    
    public KubeJSGoety() {
        ResearchNetwork.init();
        MinecraftForge.EVENT_BUS.register(ResearchGameplayEvents.class);
    }
}
