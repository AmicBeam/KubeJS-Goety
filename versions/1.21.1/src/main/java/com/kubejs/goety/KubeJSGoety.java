package com.kubejs.goety;

import com.kubejs.goety.research.ResearchNetwork;
import com.kubejs.goety.plugin.GoetyKubeJSPlugin;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(KubeJSGoety.MOD_ID)
public class KubeJSGoety {
    public static final String MOD_ID = "kubejs_goety";

    public KubeJSGoety(IEventBus modEventBus) {
        ResearchNetwork.init(modEventBus);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> GoetyKubeJSPlugin.clearServerContext());
    }
}
