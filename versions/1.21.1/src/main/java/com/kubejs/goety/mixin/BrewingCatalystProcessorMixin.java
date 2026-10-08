package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.compat.patchouli.BrewingCatalystProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BrewingCatalystProcessor.class, remap = false)
public class BrewingCatalystProcessorMixin {
    @Redirect(
            method = {"setup", "process"},
            at = @At(value = "NEW", target = "com/Polarice3/Goety/common/effects/brew/BrewEffects", remap = false),
            remap = false
    )
    private BrewEffects kubejs_goety$useSingletonBrewEffects() {
        return BrewEffects.INSTANCE;
    }
}
