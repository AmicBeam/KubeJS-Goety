package com.kubejs.goety.event;

import com.Polarice3.Goety.api.ritual.RitualType;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;

/**
 * 删除仪式类型的事件
 * 
 * 在脚本中使用：
 * GoetyEvents.removeRitual(event => {
 *     event.remove('storm');
 * });
 */
@Info("用于删除仪式类型")
public class RemoveRitualEventJS extends EventJS {
    
    /**
     * 删除仪式类型
     * 
     * @param ritualId 要删除的仪式 ID
     */
    @Info(value = "删除指定的仪式类型", params = {
        @Param(name = "ritualId", value = "要删除的仪式 ID（字符串）")
    })
    public void remove(String ritualId) {
        if (ritualId == null || ritualId.isEmpty()) {
            console.error("仪式 ID 不能为空");
            return;
        }
        
        // 检查是否是内置仪式（无法删除）
        java.util.Map<String, com.Polarice3.Goety.api.ritual.IRitualType> builtinRituals = new java.util.HashMap<>();
        builtinRituals.put("animation", RitualType.ANIMATION);
        builtinRituals.put("necroturgy", RitualType.NECROTURGY);
        builtinRituals.put("forge", RitualType.FORGE);
        builtinRituals.put("magic", RitualType.MAGIC);
        builtinRituals.put("adept_nether", RitualType.ADEPT_NETHER);
        builtinRituals.put("expert_nether", RitualType.EXPERT_NETHER);
        builtinRituals.put("sabbath", RitualType.SABBATH);
        builtinRituals.put("end", RitualType.END);
        builtinRituals.put("sky", RitualType.SKY);
        builtinRituals.put("storm", RitualType.STORM);
        builtinRituals.put("geoturgy", RitualType.GEOTURGY);
        builtinRituals.put("frost", RitualType.FROST);
        builtinRituals.put("deep", RitualType.DEEP);
        
        if (builtinRituals.containsKey(ritualId)) {
            console.warn("无法删除内置仪式类型 '" + ritualId + "'。如果需要禁用，请使用 modifyRitual 返回 false");
            return;
        }
        
        // 从自定义列表中移除
        if (RitualType.RITUAL_TYPE_LIST.containsKey(ritualId)) {
            RitualType.RITUAL_TYPE_LIST.remove(ritualId);
            console.info("✓ 已删除仪式类型: " + ritualId);
        } else {
            console.warn("仪式类型 '" + ritualId + "' 不存在或未被注册");
        }
    }
    
    /**
     * 删除多个仪式类型
     * 
     * @param ritualIds 要删除的仪式 ID 数组
     */
    @Info(value = "删除多个仪式类型", params = {
        @Param(name = "ritualIds", value = "要删除的仪式 ID 数组")
    })
    public void removeAll(String... ritualIds) {
        for (String ritualId : ritualIds) {
            remove(ritualId);
        }
    }
}

