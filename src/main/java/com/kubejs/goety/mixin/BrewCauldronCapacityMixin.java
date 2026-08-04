package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.blocks.entities.BrewCauldronBlockEntity;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import com.Polarice3.Goety.common.effects.brew.modifiers.CapacityModifier;
import com.Polarice3.Goety.utils.BrewUtils;
import com.kubejs.goety.brew.BrewData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BrewCauldronBlockEntity.class, remap = false, priority = 500)
public abstract class BrewCauldronCapacityMixin extends BlockEntity implements WorldlyContainer {
    @Shadow(remap = false)
    public BrewCauldronBlockEntity.Mode mode;
    @Shadow(remap = false)
    public int capacity;
    @Shadow(remap = false)
    public int duration;
    @Shadow(remap = false)
    public int amplifier;
    @Shadow(remap = false)
    public int aoe;
    @Shadow(remap = false)
    public float lingering;
    @Shadow(remap = false)
    public int quaff;
    @Shadow(remap = false)
    public float velocity;
    @Shadow(remap = false)
    public boolean isAquatic;
    @Shadow(remap = false)
    public boolean isFireProof;

    public BrewCauldronCapacityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow(remap = false)
    protected abstract int getFirstEmptySlot();

    @Shadow(remap = false)
    public abstract int getCapacity();

    @Shadow(remap = false)
    public abstract int getDuration();

    @Shadow(remap = false)
    public abstract void multiplyCost(float cost);

    @Shadow(remap = false)
    public abstract int getAmplifier();

    @Shadow(remap = false)
    public abstract int getAoE();

    @Shadow(remap = false)
    public abstract float getLingering();

    @Shadow(remap = false)
    public abstract int getQuaff();

    @Shadow(remap = false)
    public abstract float getVelocity();

    @Shadow(remap = false)
    public abstract boolean isAquatic();

    @Shadow(remap = false)
    public abstract boolean isFireProof();

    @Shadow(remap = false)
    public abstract BrewCauldronBlockEntity.Mode fail();

    @Shadow(remap = false)
    public abstract boolean freeModifier(BrewModifier brewModifier);

    @Shadow(remap = false)
    public abstract int getOccupiedSlots();

    @Shadow(remap = false)
    public abstract void clearContent();

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 32))
    private int kubejs_goety$expandContainerSize(int original) {
        return Math.max(original, BrewData.getMaxCapacity());
    }

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true)
    private void kubejs_goety$applyScriptedLevels(
            ItemStack itemStack,
            CallbackInfoReturnable<BrewCauldronBlockEntity.Mode> callback
    ) {
        if (this.level == null || this.level.isClientSide || this.mode == BrewCauldronBlockEntity.Mode.CRAFTING) {
            return;
        }

        BrewModifier modifier = BrewEffects.INSTANCE.getModifier(itemStack.getItem());
        if (modifier == null) {
            return;
        }

        if (this.mode == BrewCauldronBlockEntity.Mode.IDLE && modifier instanceof CapacityModifier
                && modifier.getLevel() == 0) {
            int initialCapacity = BrewData.getInitialCapacity();
            if (this.getCapacity() < initialCapacity) {
                this.clearContent();
                this.capacity = Math.min(BrewData.MAX_CAULDRON_CAPACITY, initialCapacity);
                this.kubejs_goety$spawnActivationParticles();
                callback.setReturnValue(BrewCauldronBlockEntity.Mode.BREWING);
            } else {
                callback.setReturnValue(this.fail());
            }
            return;
        }

        if (this.mode != BrewCauldronBlockEntity.Mode.BREWING) {
            return;
        }

        boolean capacityModifier = modifier instanceof CapacityModifier;
        boolean levelableAugmentation = BrewData.isLevelableAugmentation(modifier.getId());
        if (!capacityModifier && !levelableAugmentation) {
            return;
        }

        int firstEmpty = this.getFirstEmptySlot();
        boolean activate = capacityModifier && modifier.getLevel() == 0;
        if (firstEmpty == -1
                || (!this.freeModifier(modifier) && !activate && this.getOccupiedSlots() >= this.getCapacity())) {
            callback.setReturnValue(this.fail());
            return;
        }

        this.setItem(firstEmpty, itemStack);
        if (capacityModifier && !BrewUtils.hasEffect(this.getBrew())) {
            if (this.kubejs_goety$applyCapacityLevel((CapacityModifier) modifier)) {
                callback.setReturnValue(BrewCauldronBlockEntity.Mode.BREWING);
            } else {
                callback.setReturnValue(this.fail());
            }
            return;
        }

        if (levelableAugmentation && BrewUtils.hasEffect(this.getBrew())) {
            if (this.kubejs_goety$applyLevelableAugmentation(modifier)) {
                callback.setReturnValue(BrewCauldronBlockEntity.Mode.BREWING);
            } else {
                callback.setReturnValue(this.fail());
            }
            return;
        }

        callback.setReturnValue(this.fail());
    }

    @Shadow(remap = false)
    public abstract ItemStack getBrew();

    @Unique
    private boolean kubejs_goety$applyCapacityLevel(CapacityModifier modifier) {
        int targetLevel = modifier.getLevel();
        if (targetLevel < 1 || targetLevel > BrewData.getMaxCapacityLevel()) {
            return false;
        }

        int expected = BrewData.getInitialCapacity() + BrewData.getCapacityPrefixSum(targetLevel - 1);
        if (this.getCapacity() != expected) {
            return false;
        }

        this.capacity = Math.min(
                BrewData.MAX_CAULDRON_CAPACITY,
                this.capacity + BrewData.getCapacityDelta(targetLevel)
        );
        this.clearContent();
        return true;
    }

    @Unique
    private void kubejs_goety$spawnActivationParticles() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int k = 0; k < 20; ++k) {
            float radius = serverLevel.random.nextFloat() * 4.0F;
            float angle = serverLevel.random.nextFloat() * ((float) Math.PI * 2F);
            double xSpeed = Mth.cos(angle) * radius;
            double ySpeed = 0.01D + serverLevel.random.nextDouble() * 0.5D;
            double zSpeed = Mth.sin(angle) * radius;
            serverLevel.sendParticles(
                    ParticleTypes.WITCH,
                    (this.getBlockPos().getX() + 0.5D) + xSpeed * 0.1D,
                    (this.getBlockPos().getY() + 0.5D) + 0.3D,
                    (this.getBlockPos().getZ() + 0.5D) + zSpeed * 0.1D,
                    0,
                    xSpeed,
                    ySpeed,
                    zSpeed,
                    0.25F
            );
        }
    }

    @Unique
    private boolean kubejs_goety$applyLevelableAugmentation(BrewModifier brewModifier) {
        String id = brewModifier.getId();
        int modLevel = brewModifier.getLevel();
        BrewData.AugmentationLevel level = BrewData.getAugmentationLevel(id, modLevel);
        if (level == null) {
            return false;
        }

        float expected = BrewData.getAugmentationValuePrefix(id, modLevel);
        if (id.equals(BrewModifier.DURATION) && this.kubejs_goety$matchesIntValue(this.getDuration(), expected)) {
            this.duration += Math.round(level.value());
            this.multiplyCost(level.cost());
            return true;
        }
        if (id.equals(BrewModifier.AMPLIFIER) && this.kubejs_goety$matchesIntValue(this.getAmplifier(), expected)) {
            this.amplifier += Math.round(level.value());
            this.multiplyCost(level.cost());
            return true;
        }
        if (id.equals(BrewModifier.AOE) && this.kubejs_goety$matchesIntValue(this.getAoE(), expected)) {
            this.aoe += Math.round(level.value());
            this.multiplyCost(level.cost());
            return true;
        }
        if (id.equals(BrewModifier.LINGER) && this.kubejs_goety$matchesFloatValue(this.getLingering(), expected)) {
            this.lingering += level.value();
            this.multiplyCost(level.cost());
            return true;
        }
        if (id.equals(BrewModifier.QUAFF) && this.kubejs_goety$matchesIntValue(this.getQuaff(), expected)) {
            this.quaff += Math.round(level.value());
            this.multiplyCost(level.cost());
            return true;
        }
        if (id.equals(BrewModifier.VELOCITY) && this.kubejs_goety$matchesFloatValue(this.getVelocity(), expected)) {
            this.velocity += level.value();
            this.multiplyCost(level.cost());
            return true;
        }
        return false;
    }

    @Unique
    private boolean kubejs_goety$matchesIntValue(int current, float expected) {
        return current == Math.round(expected);
    }

    @Unique
    private boolean kubejs_goety$matchesFloatValue(float current, float expected) {
        return Math.abs(current - expected) < 0.0001F;
    }
}
