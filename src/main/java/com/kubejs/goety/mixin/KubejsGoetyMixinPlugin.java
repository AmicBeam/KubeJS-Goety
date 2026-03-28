package com.kubejs.goety.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Set;

public class KubejsGoetyMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (isModLoadedEarly("revelationfix")
                && (mixinClassName.endsWith("BrewCauldronBlockEntityMixin")
                || mixinClassName.endsWith("BrewCauldronCapacityMixin"))) {
            return false;
        }
        return true;
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

    private boolean isModLoadedEarly(String modId) {
        try {
            LoadingModList loadingModList = FMLLoader.getLoadingModList();
            if (loadingModList == null) {
                return false;
            }
            return loadingModList.getModFiles().stream()
                    .flatMap(modFileInfo -> modFileInfo.getMods().stream())
                    .map(IModInfo::getModId)
                    .anyMatch(modId::equals);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
