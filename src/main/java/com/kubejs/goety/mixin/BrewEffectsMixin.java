package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import com.Polarice3.Goety.common.effects.brew.modifiers.CapacityModifier;
import com.kubejs.goety.brew.BrewData;
import com.kubejs.goety.bridge.BrewEffectsInvoker;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = BrewEffects.class, remap = false)
public abstract class BrewEffectsMixin implements BrewEffectsInvoker {
    @Shadow
    @Final
    private Map<Item, BrewModifier> modifiers;

    @Override
    public void forceModifierRegister_(BrewModifier modifier, Item ingredient) {
        this.modifiers.put(ingredient, modifier);
        if (modifier instanceof CapacityModifier) {
            for (Item removed : BrewData.replaceCapacity(ingredient, modifier.level)) {
                this.modifiers.remove(removed);
            }
        } else {
            BrewData.registerAugmentation(ingredient, modifier.id, modifier.level);
        }
    }
}
