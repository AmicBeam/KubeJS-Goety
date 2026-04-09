package com.kubejs.goety.mixin;

import com.Polarice3.Goety.common.blocks.entities.BrewCauldronBlockEntity;
import com.Polarice3.Goety.common.crafting.BrewingRecipe;
import com.Polarice3.Goety.common.crafting.ModRecipeSerializer;
import com.Polarice3.Goety.common.effects.brew.BrewEffect;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.effects.brew.PotionBrewEffect;
import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import com.Polarice3.Goety.common.effects.brew.modifiers.CapacityModifier;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.utils.BrewUtils;
import com.kubejs.goety.brew.BrewData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BrewCauldronBlockEntity.class, remap = false)
public abstract class BrewCauldronCapacityMixin extends BlockEntity implements WorldlyContainer {
    @Shadow(remap = false)
    public BrewCauldronBlockEntity.Mode mode;
    @Shadow(remap = false)
    public int capacity;
    @Shadow(remap = false)
    public int capacityUsed;
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
    public abstract boolean hasNoAugmentation();

    @Shadow(remap = false)
    public abstract int getCapacityUsed();

    @Shadow(remap = false)
    public abstract void addCost(float cost);

    @Shadow(remap = false)
    public abstract void setColor(int color);

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
    public abstract void markUpdated();

    @Shadow(remap = false)
    public abstract BrewCauldronBlockEntity.Mode fail();

    @Shadow(remap = false)
    public abstract boolean freeModifier(BrewModifier brewModifier);

    @Shadow(remap = false)
    public abstract int getOccupiedSlots();

    @Shadow(remap = false)
    public abstract ItemStack getBrew();

    @Shadow(remap = false)
    public abstract void clearContent();

    @Shadow
    public abstract @NotNull ItemStack getItem(int pIndex);

    @Shadow(remap = false)
    public abstract EntityType<?> getSacrificed(int pIndex);

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 32))
    private int kubejs_goety$expandContainerSize(int original) {
        return Math.max(original, BrewData.getMaxCapacity());
    }

    @Overwrite(remap = false)
    public BrewCauldronBlockEntity.Mode insertItem(ItemStack itemStack) {
        if (this.level != null && !this.level.isClientSide) {
            Item ingredient = itemStack.getItem();
            BrewModifier brewModifier = BrewEffects.INSTANCE.getModifier(ingredient);
            int modLevel = brewModifier != null ? brewModifier.getLevel() : -1;
            boolean activate = brewModifier instanceof CapacityModifier && brewModifier.getLevel() == 0;
            int firstEmpty = getFirstEmptySlot();
            if (firstEmpty != -1) {
                if (this.mode == BrewCauldronBlockEntity.Mode.BREWING && !this.freeModifier(brewModifier) && !activate) {
                    if (this.getOccupiedSlots() >= this.getCapacity()) {
                        return fail();
                    }
                }

                this.setItem(firstEmpty, itemStack);
                int initialCapacity = BrewData.getInitialCapacity();
                if (this.mode == BrewCauldronBlockEntity.Mode.IDLE && this.getCapacity() < initialCapacity && activate) {
                    this.clearContent();
                    this.capacity = Math.min(BrewData.MAX_CAULDRON_CAPACITY, initialCapacity);
                    if (this.level instanceof ServerLevel serverLevel) {
                        for (int k = 0; k < 20; ++k) {
                            float f2 = serverLevel.random.nextFloat() * 4.0F;
                            float f1 = serverLevel.random.nextFloat() * ((float) Math.PI * 2F);
                            double d1 = Mth.cos(f1) * f2;
                            double d2 = 0.01D + serverLevel.random.nextDouble() * 0.5D;
                            double d3 = Mth.sin(f1) * f2;
                            serverLevel.sendParticles(ParticleTypes.WITCH, (this.getBlockPos().getX() + 0.5D) + d1 * 0.1D, (this.getBlockPos().getY() + 0.5D) + 0.3D, (this.getBlockPos().getZ() + 0.5D) + d3 * 0.1D, 0, d1, d2, d3, 0.25F);
                        }
                    }
                    return BrewCauldronBlockEntity.Mode.BREWING;
                }
                if (this.mode == BrewCauldronBlockEntity.Mode.BREWING) {
                    BrewingRecipe brewingRecipe = this.level.getRecipeManager().getAllRecipesFor(ModRecipeSerializer.BREWING_TYPE.get()).stream().filter(recipe -> recipe.input.test(itemStack)).findFirst().orElse(null);
                    BrewEffect brewEffect = BrewEffects.INSTANCE.getEffectFromCatalyst(ingredient);
                    if (this.hasNoAugmentation()) {
                        if (brewingRecipe != null || brewEffect != null) {
                            if (brewingRecipe != null) {
                                if ((brewingRecipe.getCapacityExtra() + this.getCapacityUsed()) <= this.getCapacity()) {
                                    this.capacityUsed += brewingRecipe.getCapacityExtra();
                                    this.addCost(brewingRecipe.soulCost);
                                    this.setColor(BrewUtils.getColor(this.getBrew()));
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewEffect != null) {
                                if ((brewEffect.getCapacityExtra() + this.getCapacityUsed()) <= this.getCapacity()) {
                                    this.capacityUsed += brewEffect.getCapacityExtra();
                                    this.addCost(brewEffect.getSoulCost());
                                    this.setColor(BrewUtils.getColor(this.getBrew()));
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                        }
                    }

                    if (brewModifier != null) {
                        if (BrewUtils.hasEffect(this.getBrew())) {
                            if (brewModifier.getId().equals(BrewModifier.HIDDEN) || brewModifier.getId().equals(BrewModifier.SPLASH) || brewModifier.getId().equals(BrewModifier.LINGERING) || brewModifier.getId().equals(BrewModifier.GAS)) {
                                if (brewModifier.getId().equals(BrewModifier.HIDDEN)) {
                                    this.addCost(10);
                                }
                                return BrewCauldronBlockEntity.Mode.BREWING;
                            }
                            if (brewModifier.getId().equals(BrewModifier.DURATION)) {
                                if (this.getDuration() == 0 && modLevel == 0) {
                                    this.duration++;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getDuration() == 1 && modLevel == 1) {
                                    this.duration++;
                                    this.multiplyCost(1.5F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getDuration() == 2 && modLevel == 2) {
                                    this.duration++;
                                    this.multiplyCost(2.0F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.AMPLIFIER)) {
                                if (this.getAmplifier() == 0 && modLevel == 0) {
                                    this.amplifier++;
                                    this.multiplyCost(2.0F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getAmplifier() == 1 && modLevel == 1) {
                                    this.amplifier++;
                                    this.multiplyCost(2.5F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getAmplifier() == 2 && modLevel == 2) {
                                    this.amplifier++;
                                    this.multiplyCost(3.0F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.AOE)) {
                                if (this.getAoE() == 0 && modLevel == 0) {
                                    this.aoe++;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getAoE() == 1 && modLevel == 1) {
                                    this.aoe++;
                                    this.multiplyCost(1.5F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getAoE() == 2 && modLevel == 2) {
                                    this.aoe++;
                                    this.multiplyCost(2.0F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.LINGER)) {
                                if (this.getLingering() == 0 && modLevel == 0) {
                                    this.lingering++;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getLingering() == 1 && modLevel == 1) {
                                    this.lingering++;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getLingering() == 2 && modLevel == 2) {
                                    this.lingering++;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.QUAFF)) {
                                if (this.getQuaff() == 0 && modLevel == 0) {
                                    this.quaff += 8;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getQuaff() == 8 && modLevel == 1) {
                                    this.quaff += 8;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getQuaff() == 16 && modLevel == 2) {
                                    this.quaff += 8;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.VELOCITY)) {
                                if (this.getVelocity() == 0 && modLevel == 0) {
                                    this.velocity += 0.1F;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getVelocity() == 0.1F && modLevel == 1) {
                                    this.velocity += 0.2F;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                                if (this.getVelocity() == 0.3F && modLevel == 2) {
                                    this.velocity += 0.2F;
                                    this.multiplyCost(1.25F);
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.AQUATIC)) {
                                if (!this.isAquatic() && modLevel == 0) {
                                    this.isAquatic = true;
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                            if (brewModifier.getId().equals(BrewModifier.FIRE_PROOF)) {
                                if (!this.isFireProof() && modLevel == 0) {
                                    this.isFireProof = true;
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                        } else if (brewModifier instanceof CapacityModifier capacityModifier) {
                            int targetLevel = capacityModifier.getLevel();
                            int maxLevel = BrewData.getMaxCapacityLevel();
                            if (targetLevel >= 1 && targetLevel <= maxLevel) {
                                int initial = BrewData.getInitialCapacity();
                                int expected = initial + BrewData.getCapacityPrefixSum(targetLevel - 1);
                                if (this.getCapacity() == expected) {
                                    this.capacity = Math.min(BrewData.MAX_CAULDRON_CAPACITY, this.capacity + BrewData.getCapacityDelta(targetLevel));
                                    this.clearContent();
                                    return BrewCauldronBlockEntity.Mode.BREWING;
                                }
                            }
                        }
                    }
                }
            } else if (this.mode == BrewCauldronBlockEntity.Mode.IDLE && this.getCapacity() < BrewData.getInitialCapacity() && activate) {
                this.clearContent();
                this.capacity = Math.min(BrewData.MAX_CAULDRON_CAPACITY, BrewData.getInitialCapacity());
                if (this.level instanceof ServerLevel serverLevel) {
                    for (int k = 0; k < 20; ++k) {
                        float f2 = serverLevel.random.nextFloat() * 4.0F;
                        float f1 = serverLevel.random.nextFloat() * ((float) Math.PI * 2F);
                        double d1 = Mth.cos(f1) * f2;
                        double d2 = 0.01D + serverLevel.random.nextDouble() * 0.5D;
                        double d3 = Mth.sin(f1) * f2;
                        serverLevel.sendParticles(ParticleTypes.WITCH, (this.getBlockPos().getX() + 0.5D) + d1 * 0.1D, (this.getBlockPos().getY() + 0.5D) + 0.3D, (this.getBlockPos().getZ() + 0.5D) + d3 * 0.1D, 0, d1, d2, d3, 0.25F);
                    }
                }
                return BrewCauldronBlockEntity.Mode.BREWING;
            }
            this.markUpdated();
        }
        return fail();
    }
}
