package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.blocks.entities.BrewCauldronBlockEntity;
import com.kubejs.goety.brew.BrewData;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BrewCauldronBlockEntity.class, remap = false, priority = 500)
public class BrewCauldronCraftingStarterMixin {
    @ModifyArg(
            method = "insertItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
                    ordinal = 0,
                    remap = true
            ),
            index = 0,
            remap = false
    )
    private Item kubejs_goety$replaceCauldronCraftingStarter(Item original) {
        return BrewData.getCauldronStarter(original);
    }
}
