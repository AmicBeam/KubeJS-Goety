package com.kubejs.goety.bridge;

import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import net.minecraft.world.item.Item;

public interface BrewEffectsInvoker {
    /** Method name/signature referenced RevelationFix's corresponding bridge; see NOTICE.md. */
    void forceModifierRegister_(BrewModifier modifier, Item ingredient);

    BrewModifier removeModifier_(Item ingredient);
}
