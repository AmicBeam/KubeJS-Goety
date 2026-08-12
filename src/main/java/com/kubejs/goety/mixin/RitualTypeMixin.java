package com.kubejs.goety.mixin;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.api.ritual.RitualType;
import com.kubejs.goety.ritual.RitualOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RitualType.class, remap = false, priority = 1500)
public class RitualTypeMixin {
    @Inject(method = "getRitualType", at = @At("HEAD"), cancellable = true, remap = false)
    private static void kubejs_goety$resolveOverride(String id, CallbackInfoReturnable<IRitualType> callback) {
        IRitualType override = RitualOverrides.get(id);
        if (override != null) {
            callback.setReturnValue(override);
        }
    }
}
