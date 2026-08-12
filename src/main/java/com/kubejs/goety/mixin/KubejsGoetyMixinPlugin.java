package com.kubejs.goety.mixin;

import com.kubejs.goety.compat.EarlyModCompatibility;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class KubejsGoetyMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        EarlyModCompatibility.logDecision();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !isCauldronMixin(mixinClassName) || !EarlyModCompatibility.shouldDisableCauldronMixins();
    }

    private static boolean isCauldronMixin(String mixinClassName) {
        return mixinClassName.endsWith("BrewCauldronBlockEntityMixin")
                || mixinClassName.endsWith("BrewCauldronCapacityMixin")
                || mixinClassName.endsWith("BrewCauldronCraftingStarterMixin");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
