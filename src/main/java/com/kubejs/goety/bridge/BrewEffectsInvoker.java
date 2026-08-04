package com.kubejs.goety.bridge;

import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import net.minecraft.world.item.Item;

/**
 * Bridge for updating Goety's private brew modifier registry.
 *
 * <p>The initial bridge design was developed with reference to RevelationFix's
 * corresponding invoker. RevelationFix declares ARR (All Rights Reserved);
 * see the repository's {@code NOTICE.md}.</p>
 */
public interface BrewEffectsInvoker {
    void forceModifierRegister_(BrewModifier modifier, Item ingredient);

    BrewModifier removeModifier_(Item ingredient);
}
