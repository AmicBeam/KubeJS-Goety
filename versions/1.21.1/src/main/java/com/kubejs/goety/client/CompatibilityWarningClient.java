package com.kubejs.goety.client;

import com.kubejs.goety.KubeJSGoety;
import com.kubejs.goety.compat.EarlyModCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class CompatibilityWarningClient {
    private CompatibilityWarningClient() {
    }

    private static Component message() {
        return Component.translatable("warning.kubejs_goety.cauldron_compat.message",
                EarlyModCompatibility.getConflictSummary());
    }

    @EventBusSubscriber(modid = KubeJSGoety.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (!EarlyModCompatibility.shouldDisableCauldronMixins()) {
                return;
            }
            event.enqueueWork(() -> SystemToast.add(
                    Minecraft.getInstance().getToasts(),
                    SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                    Component.translatable("warning.kubejs_goety.cauldron_compat.title"),
                    message()
            ));
        }
    }

    @EventBusSubscriber(modid = KubeJSGoety.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            if (EarlyModCompatibility.shouldDisableCauldronMixins()) {
                event.getPlayer().displayClientMessage(message(), false);
            }
        }
    }
}
