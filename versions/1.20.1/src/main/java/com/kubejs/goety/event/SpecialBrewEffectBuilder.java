package com.kubejs.goety.event;

import com.Polarice3.Goety.common.effects.brew.BrewEffect;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.world.item.Item;

import java.lang.reflect.Method;

/**
 * 特殊效果 brewing 配方的构建器，支持链式调用
 * 
 * 使用示例：
 * event.addSpecialBrewEffect('minecraft:bone_meal', 'grow')
 *     .soulCost(10)
 *     .capacityExtra(2);
 */
public class SpecialBrewEffectBuilder {
    private final Item item;
    private final String effectType;
    private int soulCost = 25;
    private int capacityExtra = 0;
    private final RegisterBrewEventJS parent;
    
    public SpecialBrewEffectBuilder(RegisterBrewEventJS parent, Item item, String effectType) {
        this.parent = parent;
        this.item = item;
        this.effectType = effectType;
    }
    
    /**
     * 设置灵魂消耗
     */
    public SpecialBrewEffectBuilder soulCost(Object cost) {
        if (cost != null) {
            this.soulCost = ((Number) UtilsJS.cast(cost)).intValue();
        }
        return this;
    }
    
    /**
     * 设置额外容量
     */
    public SpecialBrewEffectBuilder capacityExtra(Object extra) {
        if (extra != null) {
            this.capacityExtra = ((Number) UtilsJS.cast(extra)).intValue();
        }
        return this;
    }
    
    /**
     * 完成构建并注册效果
     * 在 KubeJS 中，链式调用通常不需要显式调用此方法
     * 但为了确保注册，建议在链式调用的最后调用此方法
     */
    public void build() {
        register();
    }
    
    /**
     * 内部注册方法
     */
    private void register() {
        try {
            BrewEffect brewEffect = parent.createSpecialBrewEffect(effectType.toLowerCase(), soulCost, capacityExtra);
            if (brewEffect != null) {
                // 使用反射调用 register 方法
                Method registerMethod = BrewEffects.class.getDeclaredMethod("register", BrewEffect.class, Item.class);
                registerMethod.setAccessible(true);
                registerMethod.invoke(BrewEffects.INSTANCE, brewEffect, item);
                ScriptType.SERVER.console.info("✓ 已注册特殊效果: " + item + " -> " + effectType + 
                    " (灵魂消耗: " + soulCost + ", 额外容量: " + capacityExtra + ")");
            } else {
                ScriptType.SERVER.console.error("无效的效果类型: " + effectType);
            }
        } catch (Exception e) {
            ScriptType.SERVER.console.error("注册特殊效果失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * toString 方法，当对象被转换为字符串时自动注册
     * 这允许链式调用在脚本执行时自动完成注册
     */
    @Override
    public String toString() {
        register();
        return "SpecialBrewEffectBuilder{item=" + item + ", effectType=" + effectType + 
            ", soulCost=" + soulCost + ", capacityExtra=" + capacityExtra + "}";
    }
}

