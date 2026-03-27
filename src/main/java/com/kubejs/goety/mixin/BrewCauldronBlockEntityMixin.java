package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.blocks.entities.BrewCauldronBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BrewCauldronBlockEntity.class, remap = false)
public class BrewCauldronBlockEntityMixin {
    @Redirect(
            method = {"insertItem", "getBrew"},
            at = @At(value = "NEW", target = "com/Polarice3/Goety/common/effects/brew/BrewEffects", remap = false),
            remap = false
    )
    private BrewEffects kubejs_goety$useSingletonBrewEffects() {
        return BrewEffects.INSTANCE;
    }
}
