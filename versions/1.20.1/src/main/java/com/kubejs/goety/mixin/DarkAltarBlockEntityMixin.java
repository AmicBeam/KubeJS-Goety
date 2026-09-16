package com.kubejs.goety.mixin;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.api.ritual.RitualType;
import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.kubejs.goety.event.StartAwareRitualType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DarkAltarBlockEntity.class, remap = false)
public abstract class DarkAltarBlockEntityMixin extends BlockEntity {
    protected DarkAltarBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "startRitual", at = @At("TAIL"))
    private void kubejs_goety$dispatchRitualStart(Player castingPlayer, ItemStack activationItem,
                                                  RitualRecipe recipe, CallbackInfo callback) {
        if (this.level == null || this.level.isClientSide || recipe == null || recipe.getCraftType() == null) {
            return;
        }

        DarkAltarBlockEntity altar = (DarkAltarBlockEntity) (Object) this;
        if (altar.getCurrentRitualRecipe() != recipe) {
            return;
        }

        ItemStack activationSnapshot = altar.itemStackHandler
                .map(handler -> handler.getStackInSlot(0).copy())
                .orElse(ItemStack.EMPTY);
        for (IRitualType ritualType : RitualType.getAllRitualType()) {
            if (ritualType instanceof StartAwareRitualType startAware
                    && recipe.getCraftType().contains(ritualType.getName())) {
                startAware.onStartRitual(this.level, altar.getBlockPos(), altar, castingPlayer, activationSnapshot);
            }
        }
    }
}
