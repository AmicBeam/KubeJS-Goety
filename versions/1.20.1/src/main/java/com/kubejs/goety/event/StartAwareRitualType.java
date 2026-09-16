package com.kubejs.goety.event;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Internal extension used by the dark altar mixin to dispatch a callback once
 * Goety has actually accepted and started a ritual recipe.
 */
public interface StartAwareRitualType extends IRitualType {
    void onStartRitual(Level world, BlockPos darkAltarPos, DarkAltarBlockEntity tileEntity,
                       Player castingPlayer, ItemStack activationItem);
}
