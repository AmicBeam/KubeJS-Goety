package com.kubejs.goety.research;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class ResearchGameplayEvents {
    private ResearchGameplayEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResearchDefinition definition = ResearchData.definitionForScroll(itemId, false);
        if (definition == null) {
            return;
        }

        if (ResearchData.has(player, definition.id())) {
            send(player, definition.alreadyLearnedMessage(),
                    Component.translatable("message.kubejs_goety.research.already", definition.displayName()));
            finishInteraction(event);
            return;
        }

        List<String> missing = ResearchData.missingPrerequisites(player, definition);
        if (!missing.isEmpty()) {
            send(player, definition.missingPrerequisiteMessage(),
                    Component.translatable("message.kubejs_goety.research.missing", String.join(", ", missing)));
            finishInteraction(event);
            return;
        }

        if (ResearchData.grant(player, definition.id())) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack.copy());
            }
            send(player, definition.learnMessage(),
                    Component.translatable("message.kubejs_goety.research.learned", definition.displayName()));
            if (definition.consumeScroll() && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        finishInteraction(event);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        ResearchDefinition definition = ResearchData.definitionForScroll(itemId, true);
        if (definition == null) {
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.kubejs_goety.research_scroll",
                definition.displayName()).withStyle(ChatFormatting.GOLD));
        if (player != null && ResearchData.has(player, definition.id())) {
            event.getToolTip().add(Component.translatable("tooltip.kubejs_goety.research.learned")
                    .withStyle(ChatFormatting.BLUE));
        } else if (player != null) {
            List<String> missing = ResearchData.missingPrerequisites(player, definition);
            if (!missing.isEmpty()) {
                event.getToolTip().add(Component.translatable("tooltip.kubejs_goety.research.requires",
                        String.join(", ", missing)).withStyle(ChatFormatting.RED));
            } else {
                event.getToolTip().add(Component.translatable("tooltip.kubejs_goety.research.read")
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ResearchNetwork.sync(serverPlayer);
        }
    }

    private static void send(Player player, String custom, Component fallback) {
        // Match Goety's native ResearchScroll behavior: research feedback is
        // transient and appears above the hotbar rather than in chat history.
        player.displayClientMessage(custom == null ? fallback : Component.literal(custom), true);
    }

    private static void finishInteraction(PlayerInteractEvent.RightClickItem event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
