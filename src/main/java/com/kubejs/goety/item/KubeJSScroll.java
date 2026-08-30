package com.kubejs.goety.item;

import com.Polarice3.Goety.Goety;
import com.Polarice3.Goety.common.items.research.Scroll;
import com.Polarice3.Goety.common.research.Research;
import com.Polarice3.Goety.utils.SEHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 自定义研究卷轴物品（模组内置默认文案，玩家 lang 可覆盖）
 * <p>
 * 与 Goety 原版 {@link Scroll} 的区别：
 * <ul>
 *   <li>涉及研究 ID 的动态键（{@code info.goety.research.<id>} 右键习得消息、
 *       {@code info.goety.items.<id>} 卷轴介绍）使用 {@code translatableWithFallback} ——
 *       玩家在任何 lang 文件里写了翻译就用玩家的，没写则显示本类内置的默认文案；
 *       固定键 {@code info.goety.items.scroll}（使用提示）由模组的 zh_cn / en_us lang 文件提供。</li>
 *   <li>{@code consumable} 可选：默认 true（习得后消耗，与 Goety 原版一致）；
 *       设为 false 则习得后不消耗物品，卷轴可反复使用。</li>
 * </ul>
 */
public class KubeJSScroll extends Scroll {
    // 动态键默认文案（中/英）。插件在 ClientEvents.LANG 中按当前语言注册这些默认值，
    // 玩家在自己的 lang 文件中写同名键即可覆盖；此处的英文默认值作为组件 fallback 兜底。
    public static final String DEFAULT_LEARN_ZH = "你从卷轴中习得了新的知识……";
    public static final String DEFAULT_LEARN_EN = "You learned new knowledge from the scroll...";
    public static final String DEFAULT_DESC_ZH = "一段被遗忘的记载……";
    public static final String DEFAULT_DESC_EN = "A scroll of forgotten knowledge...";

    /** 习得成功后是否消耗物品（默认 true，与 Goety 原版一致） */
    private final boolean consumable;

    public KubeJSScroll(Properties properties, Research research, boolean consumable) {
        super(properties, research);
        this.consumable = consumable;
    }

    /**
     * 复刻 Goety 原版 {@link Scroll#use} 逻辑，仅将习得后的 shrink 改为按 consumable 决定。
     * 已学分支与原版一致：显示 already 提示，不消耗。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        if (!worldIn.isClientSide) {
            if (!SEHelper.hasResearch(playerIn, this.research)) {
                if (SEHelper.addResearch(playerIn, this.research)) {
                    CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) playerIn, itemstack);
                    if (researchGet() != null) {
                        playerIn.displayClientMessage(researchGet(), true);
                    }
                    if (consumable) {
                        itemstack.shrink(1);
                    }
                    return InteractionResultHolder.consume(playerIn.getItemInHand(handIn));
                }
            } else {
                CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) playerIn, itemstack);
                playerIn.displayClientMessage(Component.translatable("info.goety.research.already"), true);
            }
        }
        return InteractionResultHolder.pass(playerIn.getItemInHand(handIn));
    }

    @Override
    public Component researchGet() {
        return Component.translatableWithFallback("info.goety.research." + research.getId(), DEFAULT_LEARN_EN);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        // 注意:不能调用 super.appendHoverText —— 那会执行 Scroll 的完整实现(介绍+提示),
        // 与下面重复。此处直接复刻 Scroll 逻辑,仅将动态键换成带 fallback 的版本。
        tooltip.add(Component.translatableWithFallback("info.goety.items." + research.getId(), DEFAULT_DESC_EN)
                .withStyle(ChatFormatting.GOLD));
        if (worldIn != null && worldIn.isClientSide) {
            if (SEHelper.hasResearch(Goety.PROXY.getPlayer(), this.research)) {
                tooltip.add(Component.translatable("info.goety.research.learned").withStyle(ChatFormatting.BLUE));
            } else {
                tooltip.add(Component.translatable("info.goety.items.scroll").withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
