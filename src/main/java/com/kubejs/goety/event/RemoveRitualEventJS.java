package com.kubejs.goety.event;

import com.Polarice3.Goety.api.ritual.RitualType;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.script.ScriptType;
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
            ScriptType.SERVER.console.error("Ritual ID cannot be empty");
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
            ScriptType.SERVER.console.warn("Cannot remove built-in ritual type '" + ritualId + "'. If you need to disable it, use modifyRitual to return false");
            return;
        }

        
        // 使用反射访问 RitualType 的内部 Map 来移除仪式
        try {
            java.lang.reflect.Field field = RitualType.class.getDeclaredField("RITUAL_TYPE_LIST");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, com.Polarice3.Goety.api.ritual.IRitualType> ritualMap = (java.util.Map<String, com.Polarice3.Goety.api.ritual.IRitualType>) field.get(null);
            if (ritualMap.containsKey(ritualId)) {
                ritualMap.remove(ritualId);
                ScriptType.SERVER.console.info("✓ Removed ritual type: " + ritualId);
            } else {
                ScriptType.SERVER.console.warn("Ritual type '" + ritualId + "' does not exist or is not registered");
            }
        } catch (NoSuchFieldException e) {
            // 如果字段不存在，尝试其他可能的字段名
            try {
                java.lang.reflect.Field[] fields = RitualType.class.getDeclaredFields();
                for (java.lang.reflect.Field f : fields) {
                    if (java.util.Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, com.Polarice3.Goety.api.ritual.IRitualType> ritualMap = (java.util.Map<String, com.Polarice3.Goety.api.ritual.IRitualType>) f.get(null);
                        if (ritualMap != null && ritualMap.containsKey(ritualId)) {
                            ritualMap.remove(ritualId);
                            ScriptType.SERVER.console.info("✓ Removed ritual type: " + ritualId);
                            return;
                        }
                    }
                }
                ScriptType.SERVER.console.warn("Ritual type '" + ritualId + "' does not exist or could not find ritual map");
            } catch (Exception e2) {
                ScriptType.SERVER.console.error("Failed to remove ritual type '" + ritualId + "': " + e2.getMessage());
                e2.printStackTrace();
            }
        } catch (Exception e) {
            ScriptType.SERVER.console.error("Failed to remove ritual type '" + ritualId + "': " + e.getMessage());
            e.printStackTrace();
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
