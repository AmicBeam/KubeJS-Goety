package com.kubejs.goety.event;

import com.Polarice3.Goety.common.effects.brew.BrewEffect;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.effects.brew.PotionBrewEffect;
import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import com.Polarice3.Goety.common.effects.brew.modifiers.CapacityModifier;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

/**
 * 注册药酿配置的事件
 * 
 * 在脚本中使用：
 * GoetyEvents.registerBrew(event => {
 *     event.addCapacity('minecraft:nether_wart', 0);
 *     event.addCatalyst('minecraft:glowstone_dust', 'minecraft:night_vision', 25, 1200, 0);
 *     event.addEntityCatalyst('minecraft:zombie', 'minecraft:poison', 75, 1800, 1);
 *     event.addAugmentation('minecraft:redstone', 'duration', 0);
 * });
 */
@Info("用于注册药酿系统的配置（容量剂、催化剂、增强剂）")
public class RegisterBrewEventJS extends EventJS {
    
    private static Method registerMethod;
    private static Method registerEntityMethod;
    private static Method modifierRegisterMethod;
    
    static {
        try {
            registerMethod = BrewEffects.class.getDeclaredMethod("register", BrewEffect.class, Item.class);
            registerMethod.setAccessible(true);
            
            registerEntityMethod = BrewEffects.class.getDeclaredMethod("register", BrewEffect.class, EntityType.class);
            registerEntityMethod.setAccessible(true);
            
            modifierRegisterMethod = BrewEffects.class.getDeclaredMethod("modifierRegister", BrewModifier.class, Item.class);
            modifierRegisterMethod.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[KubeJS Goety] 无法初始化反射方法: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册容量剂
     * 
     * @param item 物品ID（字符串）或物品对象
     * @param level 等级（0-7）
     */
    @Info(value = "注册容量剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象"),
        @Param(name = "level", value = "等级（0-7）")
    })
    public void addCapacity(Object item, int level) {
        if (level < 0 || level > 7) {
            console.error("容量剂等级必须在 0-7 之间，当前值: " + level);
            return;
        }
        
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        try {
            CapacityModifier modifier = new CapacityModifier(level);
            modifierRegisterMethod.invoke(BrewEffects.INSTANCE, modifier, itemObj);
            console.info("✓ 已注册容量剂: " + itemObj + " (等级: " + level + ")");
        } catch (Exception e) {
            console.error("注册容量剂失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册物品催化剂
     * 
     * @param item 物品ID（字符串）或物品对象
     * @param effect 效果ID（字符串）
     * @param soulCost 灵魂消耗
     * @param duration 持续时间 tick（可选，默认600）
     * @param capacityExtra 额外容量（可选，默认0）
     */
    @Info(value = "注册物品催化剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象"),
        @Param(name = "effect", value = "效果ID（字符串，如 'minecraft:strength'）"),
        @Param(name = "soulCost", value = "灵魂消耗（整数）"),
        @Param(name = "duration", value = "持续时间 tick（整数，可选，默认600）"),
        @Param(name = "capacityExtra", value = "额外容量（整数，可选，默认0）")
    })
    public void addCatalyst(Object item, String effect, int soulCost, Object duration, Object capacityExtra) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        MobEffect mobEffect = getMobEffect(effect);
        if (mobEffect == null) {
            return;
        }
        
        int durationValue = duration != null ? UtilsJS.cast(duration, Number.class).intValue() : 600;
        int capacityExtraValue = capacityExtra != null ? UtilsJS.cast(capacityExtra, Number.class).intValue() : 0;
        
        if (durationValue <= 0) {
            console.error("持续时间必须大于 0，当前值: " + durationValue);
            return;
        }
        
        if (soulCost < 0) {
            console.error("灵魂消耗不能为负数，当前值: " + soulCost);
            return;
        }
        
        try {
            PotionBrewEffect brewEffect;
            if (capacityExtraValue != 0) {
                brewEffect = new PotionBrewEffect(mobEffect, soulCost, capacityExtraValue, durationValue);
            } else {
                brewEffect = new PotionBrewEffect(mobEffect, soulCost, durationValue);
            }
            registerMethod.invoke(BrewEffects.INSTANCE, brewEffect, itemObj);
            console.info("✓ 已注册物品催化剂: " + itemObj + " -> " + effect + " (灵魂消耗: " + soulCost + ", 持续时间: " + durationValue + ", 额外容量: " + capacityExtraValue + ")");
        } catch (Exception e) {
            console.error("注册物品催化剂失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册物品催化剂（简化版本，只使用必填参数）
     */
    public void addCatalyst(Object item, String effect, int soulCost) {
        addCatalyst(item, effect, soulCost, null, null);
    }
    
    /**
     * 注册物品催化剂（带持续时间）
     */
    public void addCatalyst(Object item, String effect, int soulCost, int duration) {
        addCatalyst(item, effect, soulCost, duration, null);
    }
    
    /**
     * 注册实体催化剂
     * 
     * @param entity 实体类型ID（字符串）或实体标签（字符串，以 # 开头）
     * @param effect 效果ID（字符串）
     * @param soulCost 灵魂消耗
     * @param duration 持续时间 tick（可选，默认600）
     * @param capacityExtra 额外容量（可选，默认0）
     */
    @Info(value = "注册实体催化剂", params = {
        @Param(name = "entity", value = "实体类型ID（字符串）或实体标签（字符串，以 # 开头）"),
        @Param(name = "effect", value = "效果ID（字符串）"),
        @Param(name = "soulCost", value = "灵魂消耗（整数）"),
        @Param(name = "duration", value = "持续时间 tick（整数，可选，默认600）"),
        @Param(name = "capacityExtra", value = "额外容量（整数，可选，默认0）")
    })
    public void addEntityCatalyst(Object entity, String effect, int soulCost, Object duration, Object capacityExtra) {
        MobEffect mobEffect = getMobEffect(effect);
        if (mobEffect == null) {
            return;
        }
        
        int durationValue = duration != null ? UtilsJS.cast(duration, Number.class).intValue() : 600;
        int capacityExtraValue = capacityExtra != null ? UtilsJS.cast(capacityExtra, Number.class).intValue() : 0;
        
        if (durationValue <= 0) {
            console.error("持续时间必须大于 0，当前值: " + durationValue);
            return;
        }
        
        if (soulCost < 0) {
            console.error("灵魂消耗不能为负数，当前值: " + soulCost);
            return;
        }
        
        try {
            PotionBrewEffect brewEffect;
            if (capacityExtraValue != 0) {
                brewEffect = new PotionBrewEffect(mobEffect, soulCost, capacityExtraValue, durationValue);
            } else {
                brewEffect = new PotionBrewEffect(mobEffect, soulCost, durationValue);
            }
            
            String entityStr = entity.toString();
            if (entityStr.startsWith("#")) {
                // 实体标签
                String tagStr = entityStr.substring(1);
                TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.tryParse(tagStr));
                if (tagKey != null) {
                    int count = 0;
                    // 遍历所有实体类型，检查是否匹配标签
                    for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
                        if (entityType.is(tagKey)) {
                            registerEntityMethod.invoke(BrewEffects.INSTANCE, brewEffect, entityType);
                            count++;
                        }
                    }
                    console.info("✓ 已注册实体标签催化剂: #" + tagStr + " -> " + effect + " (影响 " + count + " 个实体类型)");
                } else {
                    console.error("无效的实体标签: " + tagStr);
                }
            } else {
                // 单个实体类型
                EntityType<?> entityType = getEntityType(entityStr);
                if (entityType != null) {
                    registerEntityMethod.invoke(BrewEffects.INSTANCE, brewEffect, entityType);
                    console.info("✓ 已注册实体催化剂: " + entityStr + " -> " + effect + " (灵魂消耗: " + soulCost + ", 持续时间: " + durationValue + ", 额外容量: " + capacityExtraValue + ")");
                }
            }
        } catch (Exception e) {
            console.error("注册实体催化剂失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册实体催化剂（简化版本）
     */
    public void addEntityCatalyst(Object entity, String effect, int soulCost) {
        addEntityCatalyst(entity, effect, soulCost, null, null);
    }
    
    /**
     * 注册实体催化剂（带持续时间）
     */
    public void addEntityCatalyst(Object entity, String effect, int soulCost, int duration) {
        addEntityCatalyst(entity, effect, soulCost, duration, null);
    }
    
    /**
     * 注册增强剂
     * 
     * @param item 物品ID（字符串）或物品对象
     * @param modifier 增强类型（字符串）
     * @param level 等级
     */
    @Info(value = "注册增强剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象"),
        @Param(name = "modifier", value = "增强类型（字符串）：'capacity', 'duration', 'amplifier', 'aoe', 'linger', 'quaff', 'velocity', 'aquatic', 'fire_proof'"),
        @Param(name = "level", value = "等级（整数）")
    })
    public void addAugmentation(Object item, String modifier, int level) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        if (modifier == null || modifier.isEmpty()) {
            console.error("增强类型不能为空");
            return;
        }
        
        // 验证增强类型
        String modifierLower = modifier.toLowerCase();
        if (!isValidModifier(modifierLower)) {
            console.error("无效的增强类型: " + modifier + "，有效值: capacity, duration, amplifier, aoe, linger, quaff, velocity, aquatic, fire_proof");
            return;
        }
        
        try {
            BrewModifier brewModifier;
            if ("capacity".equals(modifierLower)) {
                brewModifier = new CapacityModifier(level);
            } else {
                brewModifier = new BrewModifier(modifierLower, level);
            }
            
            modifierRegisterMethod.invoke(BrewEffects.INSTANCE, brewModifier, itemObj);
            console.info("✓ 已注册增强剂: " + itemObj + " -> " + modifier + " (等级: " + level + ")");
        } catch (Exception e) {
            console.error("注册增强剂失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 移除容量剂
     */
    @Info(value = "移除容量剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象")
    })
    public void removeCapacity(Object item) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        // 注意：BrewEffects 没有公开的移除方法，我们需要通过反射访问私有 Map
        // 这里我们只能通过覆盖来实现移除，即注册一个 null 或新的配置
        console.warn("移除容量剂功能需要先移除原配置，然后重新注册。建议直接覆盖注册。");
    }
    
    /**
     * 移除物品催化剂
     */
    @Info(value = "移除物品催化剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象")
    })
    public void removeCatalyst(Object item) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        console.warn("移除催化剂功能需要先移除原配置，然后重新注册。建议直接覆盖注册。");
    }
    
    /**
     * 移除实体催化剂
     */
    @Info(value = "移除实体催化剂", params = {
        @Param(name = "entity", value = "实体类型ID（字符串）")
    })
    public void removeEntityCatalyst(Object entity) {
        console.warn("移除实体催化剂功能需要先移除原配置，然后重新注册。建议直接覆盖注册。");
    }
    
    /**
     * 移除增强剂
     */
    @Info(value = "移除增强剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象")
    })
    public void removeAugmentation(Object item) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        console.warn("移除增强剂功能需要先移除原配置，然后重新注册。建议直接覆盖注册。");
    }
    
    // ==================== 辅助方法 ====================
    
    private Item getItem(Object item) {
        if (item == null) {
            console.error("物品不能为 null");
            return null;
        }
        
        if (item instanceof Item) {
            return (Item) item;
        }
        
        if (item instanceof ItemStack) {
            return ((ItemStack) item).getItem();
        }
        
        if (item instanceof ItemStackJS) {
            return ((ItemStackJS) item).getItem();
        }
        
        try {
            Item wrapped = ItemWrapper.wrap(UtilsJS.getContext(), item);
            if (wrapped != null && wrapped != net.minecraft.world.item.Items.AIR) {
                return wrapped;
            }
        } catch (Exception e) {
            // 忽略
        }
        
        String itemId = item.toString();
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location != null) {
            Item found = ForgeRegistries.ITEMS.getValue(location);
            if (found != null && found != net.minecraft.world.item.Items.AIR) {
                return found;
            }
        }
        
        console.error("无法找到物品: " + item);
        return null;
    }
    
    private MobEffect getMobEffect(String effectId) {
        if (effectId == null || effectId.isEmpty()) {
            console.error("效果ID不能为空");
            return null;
        }
        
        ResourceLocation location = ResourceLocation.tryParse(effectId);
        if (location != null) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(location);
            if (effect != null) {
                return effect;
            }
        }
        
        console.error("无法找到效果: " + effectId);
        return null;
    }
    
    private EntityType<?> getEntityType(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            console.error("实体ID不能为空");
            return null;
        }
        
        ResourceLocation location = ResourceLocation.tryParse(entityId);
        if (location != null) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(location);
            if (entityType != null) {
                return entityType;
            }
        }
        
        console.error("无法找到实体类型: " + entityId);
        return null;
    }
    
    private boolean isValidModifier(String modifier) {
        return modifier.equals("capacity") ||
               modifier.equals("duration") ||
               modifier.equals("amplifier") ||
               modifier.equals("aoe") ||
               modifier.equals("linger") ||
               modifier.equals("quaff") ||
               modifier.equals("velocity") ||
               modifier.equals("aquatic") ||
               modifier.equals("fire_proof") ||
               modifier.equals("hidden") ||
               modifier.equals("splash") ||
               modifier.equals("lingering") ||
               modifier.equals("gas");
    }
}

