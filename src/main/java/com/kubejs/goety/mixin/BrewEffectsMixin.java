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
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = BrewEffects.class, remap = false)
public abstract class BrewEffectsMixin implements BrewEffectsInvoker {
    @Shadow
    @Final
    private Map<Item, BrewModifier> modifiers;

    @Unique
    @Override
    public void registerKubeJSGoetyModifier(BrewModifier modifier, Item ingredient) {
        BrewData.removeModifierItem(ingredient);
        this.modifiers.put(ingredient, modifier);
        if (modifier instanceof CapacityModifier) {
            BrewData.registerCapacity(ingredient, modifier.level);
        } else {
            BrewData.registerAugmentation(ingredient, modifier.id, modifier.level);
        }
    }

    @Unique
    @Override
    public BrewModifier removeKubeJSGoetyModifier(Item ingredient) {
        BrewData.removeModifierItem(ingredient);
        return this.modifiers.remove(ingredient);
    }
}
