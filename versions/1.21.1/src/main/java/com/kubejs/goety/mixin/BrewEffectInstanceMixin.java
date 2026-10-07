package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.effects.brew.BrewEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BrewEffectInstance.class, remap = false)
public class BrewEffectInstanceMixin {
    @Redirect(
            method = "load",
            at = @At(value = "NEW", target = "com/Polarice3/Goety/common/effects/brew/BrewEffects", remap = false),
            remap = false
    )
    private static BrewEffects kubejs_goety$useSingletonBrewEffects() {
        return BrewEffects.INSTANCE;
    }
}
