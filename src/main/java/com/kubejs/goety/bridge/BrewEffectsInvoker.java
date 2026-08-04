package com.kubejs.goety.bridge;

import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import net.minecraft.world.item.Item;

public interface BrewEffectsInvoker {
    void forceModifierRegister_(BrewModifier modifier, Item ingredient);

    BrewModifier removeModifier_(Item ingredient);
}
