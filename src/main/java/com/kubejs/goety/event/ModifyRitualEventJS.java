package com.kubejs.goety.event;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.api.ritual.RitualType;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.SizedIngredientWrapper;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.*;

/**
 * 修改现有仪式条件的事件
 * 
 * 在脚本中使用：
 * GoetyEvents.modifyRitual(event => {
 *     event.modify('storm', ritual => {
 *         ritual.range = 16;  // 扫描范围
 *         ritual.blocks = ['8x #minecraft:copper_ores', '3x minecraft:lightning_rod', '15x minecraft:chain'];  // 方块需求
 *     });
 * });
 */
@Info("用于修改现有仪式类型的条件")
public class ModifyRitualEventJS extends EventJS {
    
    /**
     * 修改现有仪式类型
     * 
     * @param ritualId 要修改的仪式 ID
     * @param modifiers 修改器函数（可变参数），每个函数代表一个条件组，多个条件组之间使用 OR 逻辑连接
     */
    @Info(value = "修改现有仪式类型的条件", params = {
        @Param(name = "ritualId", value = "要修改的仪式 ID（字符串）"),
        @Param(name = "modifiers", value = "修改器函数（可变参数），多个函数表示多个条件组（OR 逻辑）")
    })
    public void modify(String ritualId, Object... modifiers) {
        if (ritualId == null || ritualId.isEmpty()) {
            console.error("仪式 ID 不能为空");
            return;
        }
        
        if (modifiers == null || modifiers.length == 0) {
            console.error("至少需要一个修改器函数");
            return;
        }
        
        IRitualType existingRitual = RitualType.getRitualType(ritualId);
        if (existingRitual == null) {
            console.error("仪式类型 '" + ritualId + "' 不存在，无法修改");
            return;
        }
        
        try {
            // 如果只有一个修改器函数，使用原来的逻辑（向后兼容）
            if (modifiers.length == 1) {
                RitualModifierImpl ritualModifier = new RitualModifierImpl(ritualId, existingRitual, console);
                
                if (modifiers[0] instanceof dev.latvian.mods.rhino.BaseFunction) {
                    ((dev.latvian.mods.rhino.BaseFunction) modifiers[0]).call(
                        dev.latvian.mods.kubejs.util.UtilsJS.getContext(),
                        dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                        dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                        new Object[]{ritualModifier}
                    );
                } else {
                    console.error("修改器必须是函数");
                    return;
                }
                
                RitualType.addRitualType(ritualId, ritualModifier.buildRitualType());
                console.info("✓ 已修改仪式类型: " + ritualId);
            } else {
                // 多个修改器函数，创建多个条件组（OR 逻辑）
                List<RitualModifierImpl> conditionGroups = new ArrayList<>();
                
                for (int i = 0; i < modifiers.length; i++) {
                    Object modifier = modifiers[i];
                    if (modifier instanceof dev.latvian.mods.rhino.BaseFunction) {
                        RitualModifierImpl group = new RitualModifierImpl(ritualId, existingRitual, console);
                        ((dev.latvian.mods.rhino.BaseFunction) modifier).call(
                            dev.latvian.mods.kubejs.util.UtilsJS.getContext(),
                            dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                            dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                            new Object[]{group}
                        );
                        conditionGroups.add(group);
                    } else {
                        console.error("第 " + (i + 1) + " 个修改器必须是函数");
                    }
                }
                
                if (!conditionGroups.isEmpty()) {
                    RitualType.addRitualType(ritualId, buildRitualTypeWithOR(ritualId, conditionGroups));
                    console.info("✓ 已修改仪式类型: " + ritualId + "（" + conditionGroups.size() + " 个条件组，OR 逻辑）");
                }
            }
        } catch (Exception e) {
            console.error("修改仪式类型 '" + ritualId + "' 时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 构建支持 OR 逻辑的 IRitualType
     * 多个条件组之间使用 OR 连接，任意一个满足即可
     */
    private IRitualType buildRitualTypeWithOR(String ritualId, List<RitualModifierImpl> conditionGroups) {
        final List<RitualModifierImpl> finalGroups = new ArrayList<>(conditionGroups);
        final String finalRitualId = ritualId;
        
        return new IRitualType() {
            @Override
            public String getName() {
                return finalRitualId;  // 使用ritualId作为名称，不再使用条件组内部的name
            }
            
            @Override
            public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity,
                                        BlockPos pPos,
                                        Level pLevel) {
                // 遍历所有条件组，任意一个满足即可（OR 逻辑）
                for (RitualModifierImpl group : finalGroups) {
                    if (checkConditionGroup(group, pTileEntity, pPos, pLevel)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    /**
     * 检查单个条件组是否满足
     */
    private boolean checkConditionGroup(RitualModifierImpl group, 
                                      com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity,
                                      BlockPos pPos,
                                      Level pLevel) {
        try {
            // 如果有自定义检查函数，优先使用
            if (group.customRequirement instanceof dev.latvian.mods.rhino.BaseFunction) {
                dev.latvian.mods.rhino.BaseFunction func = (dev.latvian.mods.rhino.BaseFunction) group.customRequirement;
                Object result = func.call(
                    dev.latvian.mods.kubejs.util.UtilsJS.getContext(),
                    dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                    dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                    new Object[]{pTileEntity, pPos, pLevel}
                );
                return UtilsJS.cast(result, Boolean.class);
            }
            
            // 检查维度限制
            if (group.dimension != null && !group.dimension.isEmpty()) {
                ResourceKey<Level> dimensionKey = pLevel.dimension();
                String dimensionId = dimensionKey.location().toString();
                boolean containsMatch = group.dimensionContainsMatch != null ? group.dimensionContainsMatch : false;
                
                if (containsMatch) {
                    // 模糊匹配
                    if (!dimensionId.contains(group.dimension)) {
                        return false;
                    }
                } else {
                    // 精确匹配（默认）
                    if (!dimensionId.equals(group.dimension)) {
                        return false;
                    }
                }
            }
            
            // 检查天气要求
            if (group.weather != null) {
                switch (group.weather.toLowerCase()) {
                    case "thunder":
                        if (!pLevel.isThundering()) {
                            return false;
                        }
                        break;
                    case "rain":
                        if (!pLevel.isRaining()) {
                            return false;
                        }
                        break;
                    case "clear":
                        if (pLevel.isRaining() || pLevel.isThundering()) {
                            return false;
                        }
                        break;
                }
            }
            
            // 检查时间要求
            if (group.timeOfDay != null) {
                int skyDarken = pLevel.getSkyDarken();
                switch (group.timeOfDay.toLowerCase()) {
                    case "night":
                        if (skyDarken < 4) {
                            return false;
                        }
                        break;
                    case "day":
                        if (skyDarken >= 4) {
                            return false;
                        }
                        break;
                }
            }
            
            // 检查生物群系要求
            if (group.biome != null || (group.biomeType != null && "coldEnoughToSnow".equals(group.biomeType))) {
                String type = group.biomeType != null ? group.biomeType.toLowerCase() : "id";
                var currentBiome = pLevel.getBiome(pPos);
                
                if ("coldenoughtosnow".equals(type)) {
                    // 寒冷检查：coldEnoughToSnow
                    if (!currentBiome.get().coldEnoughToSnow(pPos)) {
                        return false;
                    }
                } else if ("tags".equals(type)) {
                    // 标签检查
                    boolean biomeTagMatch = false;
                    
                    if (group.biome instanceof CharSequence) {
                        String tagStr = group.biome.toString().replace("#", "");
                        var tagKey = TagKey.create(Registries.BIOME, ResourceLocation.tryParse(tagStr));
                        if (tagKey != null && currentBiome.is(tagKey)) {
                            biomeTagMatch = true;
                        }
                    } else if (group.biome instanceof List || (group.biome != null && group.biome.getClass().isArray())) {
                        List<?> list = ListJS.of(group.biome);
                        if (list != null) {
                            for (Object tag : list) {
                                if (tag instanceof CharSequence) {
                                    String tagStr = tag.toString().replace("#", "");
                                    var tagKey = TagKey.create(Registries.BIOME, ResourceLocation.tryParse(tagStr));
                                    if (tagKey != null && currentBiome.is(tagKey)) {
                                        biomeTagMatch = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!biomeTagMatch) {
                        return false;
                    }
                } else {
                    // ID 检查（默认）
                    boolean biomeIdMatch = false;
                    var biomeRegistry = pLevel.registryAccess().registry(Registries.BIOME);
                    
                    if (biomeRegistry != null) {
                        if (group.biome instanceof CharSequence) {
                            // 单个ID
                            var biomeKey = ResourceLocation.tryParse(group.biome.toString());
                            if (biomeKey != null) {
                                var biomeHolder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
                                if (biomeHolder.isPresent() && pLevel.getBiome(pPos).equals(biomeHolder.get())) {
                                    biomeIdMatch = true;
                                }
                            }
                        } else if (group.biome instanceof List || (group.biome != null && group.biome.getClass().isArray())) {
                            // 多个ID（OR 逻辑，满足任意一个即可）
                            List<?> list = ListJS.of(group.biome);
                            if (list != null) {
                                for (Object biomeId : list) {
                                    if (biomeId instanceof CharSequence) {
                                        var biomeKey = ResourceLocation.tryParse(biomeId.toString());
                                        if (biomeKey != null) {
                                            var biomeHolder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
                                            if (biomeHolder.isPresent() && pLevel.getBiome(pPos).equals(biomeHolder.get())) {
                                                biomeIdMatch = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!biomeIdMatch) {
                        return false;
                    }
                }
            }
            
            // 检查高度要求
            if (group.minY != null && pPos.getY() < group.minY) {
                return false;
            }
            if (group.maxY != null && pPos.getY() > group.maxY) {
                return false;
            }
            
            // 检查天空可见性要求
            if (group.requireSkyVisible != null) {
                boolean canSeeSky = pLevel.canSeeSky(pPos.above());
                if (group.requireSkyVisible && !canSeeSky) {
                    return false;
                }
                if (!group.requireSkyVisible && canSeeSky) {
                    return false;
                }
            }
            
            // 检查方块需求
            Map<SizedIngredient, Integer> blockRequirements = group.parseBlocks(group.blocks);
            if (!blockRequirements.isEmpty()) {
                Map<SizedIngredient, Integer> found = new HashMap<>();
                int finalRange = group.range != null ? group.range : 16;
                
                // 扫描范围内的方块
                for (int i = -finalRange; i <= finalRange; i++) {
                    for (int j = -finalRange; j <= finalRange; j++) {
                        for (int k = -finalRange; k <= finalRange; k++) {
                            BlockPos blockPos = pPos.offset(i, j, k);
                            BlockState blockState = pLevel.getBlockState(blockPos);
                            
                            // 检查每个需求
                            for (Map.Entry<SizedIngredient, Integer> entry : blockRequirements.entrySet()) {
                                SizedIngredient sizedIngredient = entry.getKey();
                                var item = blockState.getBlock().asItem();
                                if (item != null && !item.isEmpty()) {
                                    if (sizedIngredient.ingredient().test(item.getDefaultInstance())) {
                                        found.put(sizedIngredient, found.getOrDefault(sizedIngredient, 0) + 1);
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 验证是否满足所有需求
                for (Map.Entry<SizedIngredient, Integer> entry : blockRequirements.entrySet()) {
                    int required = entry.getValue();
                    int actual = found.getOrDefault(entry.getKey(), 0);
                    if (actual < required) {
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("[Goety仪式] 执行条件组检查时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 仪式修改器实现
     */
    public static class RitualModifierImpl {
        public String name;
        public Integer range;
        public Object blocks;  // 可以是字符串、数组或对象
        public String dimension;  // 可选：维度ID
        public Boolean dimensionContainsMatch;  // 可选：维度匹配模式，false（精确匹配，默认）或 true（模糊匹配）
        public Object customRequirement;  // 可选：自定义检查函数
        
        // 新增配置选项
        public String weather;  // 天气要求：'thunder', 'rain', 'clear', null（不检查）
        public String timeOfDay;  // 时间要求：'day', 'night', null（不检查）
        public Object biome;  // 生物群系要求（可以是字符串或数组）
        public String biomeType;  // 生物群系检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'coldEnoughToSnow'（寒冷检查）
        public Integer minY;  // 最小高度要求
        public Integer maxY;  // 最大高度要求
        public Boolean requireSkyVisible;  // 是否需要看到天空（true/false/null）
        
        private final String ritualId;
        private final IRitualType originalRitual;
        private final dev.latvian.mods.kubejs.util.ConsoleJS console;
        
        public RitualModifierImpl(String ritualId, IRitualType originalRitual, dev.latvian.mods.kubejs.util.ConsoleJS console) {
            this.ritualId = ritualId;
            this.originalRitual = originalRitual;
            this.console = console;
            this.name = originalRitual.getName();
            this.range = 16;  // 默认范围
            this.blocks = null;
            this.dimension = null;
            this.dimensionContainsMatch = false;  // 默认精确匹配
            this.customRequirement = null;
            this.weather = null;
            this.timeOfDay = null;
            this.biome = null;
            this.biomeType = "id";  // 默认ID类型
            this.minY = null;
            this.maxY = null;
            this.requireSkyVisible = null;
        }
        
        /**
         * 设置新的仪式名称
         */
        public RitualModifierImpl setName(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * 设置扫描范围
         */
        public RitualModifierImpl setRange(Integer range) {
            this.range = range;
            return this;
        }
        
        /**
         * 设置方块需求
         * 支持 KubeJS 写法：'9x minecraft:stone' 或 ['9x minecraft:stone', '3x minecraft:diamond_block']
         * 或对象：{ 'minecraft:stone': 9, 'minecraft:diamond_block': 3 }
         */
        public RitualModifierImpl setBlocks(Object blocks) {
            this.blocks = blocks;
            return this;
        }
        
        /**
         * 设置维度限制
         * @param dimension 维度ID
         * @param containsMatch 是否使用模糊匹配：false（精确匹配，默认）或 true（模糊匹配）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置维度限制", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "dimension", value = "维度ID，如 'minecraft:the_nether' 或 'aether'"),
            @dev.latvian.mods.kubejs.typings.Param(name = "containsMatch", value = "是否使用模糊匹配：false（精确匹配，默认）或 true（模糊匹配）")
        })
        public RitualModifierImpl setDimension(String dimension, Boolean containsMatch) {
            this.dimension = dimension;
            this.dimensionContainsMatch = (containsMatch != null) ? containsMatch : false;
            return this;
        }
        
        /**
         * 设置维度限制（精确匹配，默认）
         */
        public RitualModifierImpl setDimension(String dimension) {
            return setDimension(dimension, false);
        }
        
        /**
         * 设置自定义检查函数（可选，如果设置了会覆盖 blocks 检查）
         */
        public RitualModifierImpl setRequirement(Object requirement) {
            this.customRequirement = requirement;
            return this;
        }
        
        /**
         * 设置天气要求
         * @param weather 'thunder'（雷雨）、'rain'（下雨）、'clear'（晴天）、null（不检查）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置天气要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "weather", value = "'thunder'（雷雨）、'rain'（下雨）、'clear'（晴天）、null（不检查）")
        })
        public RitualModifierImpl setWeather(String weather) {
            this.weather = weather;
            return this;
        }
        
        /**
         * 设置时间要求
         * @param timeOfDay 'day'（白天）、'night'（夜晚）、null（不检查）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置时间要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "timeOfDay", value = "'day'（白天）、'night'（夜晚）、null（不检查）")
        })
        public RitualModifierImpl setTimeOfDay(String timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }
        
        /**
         * 设置生物群系要求
         * @param biome 生物群系值（可以是字符串或数组）
         * @param type 生物群系检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'coldEnoughToSnow'（寒冷检查）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置生物群系要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "biome", value = "生物群系值（可以是字符串或数组）。对于 'coldEnoughToSnow' 类型，此参数可以为 null"),
            @dev.latvian.mods.kubejs.typings.Param(name = "type", value = "检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'coldEnoughToSnow'（寒冷检查）")
        })
        public RitualModifierImpl setBiome(Object biome, String type) {
            this.biome = biome;
            this.biomeType = (type != null && !type.isEmpty()) ? type : "id";
            return this;
        }
        
        /**
         * 设置生物群系要求（默认ID类型）
         */
        public RitualModifierImpl setBiome(Object biome) {
            return setBiome(biome, "id");
        }
        
        /**
         * 设置最小高度要求
         * @param minY 最小Y坐标
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置最小高度要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "minY", value = "最小Y坐标")
        })
        public RitualModifierImpl setMinY(Integer minY) {
            this.minY = minY;
            return this;
        }
        
        /**
         * 设置最大高度要求
         * @param maxY 最大Y坐标
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置最大高度要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "maxY", value = "最大Y坐标")
        })
        public RitualModifierImpl setMaxY(Integer maxY) {
            this.maxY = maxY;
            return this;
        }
        
        /**
         * 设置是否需要看到天空
         * @param requireSkyVisible true（需要看到天空）、false（需要看不到天空）、null（不检查）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置是否需要看到天空", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "requireSkyVisible", value = "true（需要看到天空）、false（需要看不到天空）、null（不检查）")
        })
        public RitualModifierImpl setRequireSkyVisible(Boolean requireSkyVisible) {
            this.requireSkyVisible = requireSkyVisible;
            return this;
        }
        
        /**
         * 解析方块需求配置
         * 完全复用 KubeJS 的 SizedIngredientWrapper 来解析各种格式
         * 支持：
         * - 字符串：'minecraft:stone' 或 '9x minecraft:stone'
         * - ItemStack 对象：Item.of('minecraft:stone')，包括带 NBT 的
         * - Ingredient 对象：直接使用 KubeJS 的 Ingredient
         * - 数组：混合使用以上格式
         * - 对象/Map：key 可以是字符串或 ItemStack，value 是数量
         */
        private Map<SizedIngredient, Integer> parseBlocks(Object blocksConfig) {
            Map<SizedIngredient, Integer> requirements = new LinkedHashMap<>();
            
            if (blocksConfig == null) {
                return requirements;
            }
            
            var cx = dev.latvian.mods.kubejs.util.UtilsJS.getContext();
            
            // 如果是字符串，解析为单个需求
            if (blocksConfig instanceof CharSequence) {
                try {
                    SizedIngredient ingredient = SizedIngredientWrapper.wrap(cx, blocksConfig.toString());
                    if (ingredient != null && !ingredient.isEmpty()) {
                        // SizedIngredient 本身已经包含 count，直接使用
                        requirements.put(ingredient, ingredient.count());
                    }
                } catch (Exception e) {
                    console.error("解析方块需求失败: " + blocksConfig + " - " + e.getMessage());
                }
            }
            // 如果是数组
            else if (blocksConfig instanceof List || blocksConfig.getClass().isArray()) {
                List<?> list = ListJS.of(blocksConfig);
                if (list != null) {
                    for (Object item : list) {
                        try {
                            // 支持字符串和 ItemStack 对象（包括带 NBT 的）
                            SizedIngredient ingredient = SizedIngredientWrapper.wrap(cx, item);
                            if (ingredient != null && !ingredient.isEmpty()) {
                                requirements.put(ingredient, ingredient.count());
                            }
                        } catch (Exception e) {
                            console.error("解析方块需求失败: " + item + " - " + e.getMessage());
                        }
                    }
                }
            }
            // 如果是对象/Map
            else {
                var map = cx.optionalMapOf(blocksConfig);
                if (map != null) {
                    for (var entry : map.entrySet()) {
                        try {
                            Object key = entry.getKey();
                            int count = UtilsJS.cast(entry.getValue(), Number.class).intValue();
                            
                            // 支持 key 为字符串或 ItemStack 对象
                            var ingredient = dev.latvian.mods.kubejs.plugin.builtin.wrapper.IngredientWrapper.wrap(cx, key);
                            if (ingredient != null && !ingredient.isEmpty()) {
                                SizedIngredient sizedIngredient = SizedIngredient.of(ingredient, count);
                                requirements.put(sizedIngredient, count);
                            }
                        } catch (Exception e) {
                            console.error("解析方块需求失败: " + entry + " - " + e.getMessage());
                        }
                    }
                }
            }
            
            return requirements;
        }
        
        /**
         * 构建修改后的 IRitualType 实例
         */
        public IRitualType buildRitualType() {
            final String finalName = this.name != null ? this.name : ritualId;
            final int finalRange = this.range != null ? this.range : 16;
            final Map<SizedIngredient, Integer> blockRequirements = parseBlocks(this.blocks);
            final String finalDimension = this.dimension;
            final Boolean finalDimensionContainsMatch = this.dimensionContainsMatch;
            final Object finalCustomRequirement = this.customRequirement;
            final String finalWeather = this.weather;
            final String finalTimeOfDay = this.timeOfDay;
            final Object finalBiome = this.biome;
            final String finalBiomeType = this.biomeType;
            final Integer finalMinY = this.minY;
            final Integer finalMaxY = this.maxY;
            final Boolean finalRequireSkyVisible = this.requireSkyVisible;
            
            return new IRitualType() {
                @Override
                public String getName() {
                    return finalName;
                }
                
                @Override
                public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity, 
                                            BlockPos pPos, 
                                            Level pLevel) {
                    try {
                        // 如果有自定义检查函数，优先使用
                        if (finalCustomRequirement instanceof dev.latvian.mods.rhino.BaseFunction) {
                            dev.latvian.mods.rhino.BaseFunction func = (dev.latvian.mods.rhino.BaseFunction) finalCustomRequirement;
                            Object result = func.call(
                                dev.latvian.mods.kubejs.util.UtilsJS.getContext(),
                                dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                                dev.latvian.mods.kubejs.util.UtilsJS.getScope(),
                                new Object[]{pTileEntity, pPos, pLevel}
                            );
                            return UtilsJS.cast(result, Boolean.class);
                        }
                        
                        // 检查维度限制
                        if (finalDimension != null && !finalDimension.isEmpty()) {
                            var dimensionKey = pLevel.dimension();
                            String dimensionId = dimensionKey.location().toString();
                            boolean containsMatch = finalDimensionContainsMatch != null ? finalDimensionContainsMatch : false;
                            
                            if (containsMatch) {
                                // 模糊匹配
                                if (!dimensionId.contains(finalDimension)) {
                                    return false;
                                }
                            } else {
                                // 精确匹配（默认）
                                if (!dimensionId.equals(finalDimension)) {
                                    return false;
                                }
                            }
                        }
                        
                        // 检查天气要求
                        if (finalWeather != null) {
                            switch (finalWeather.toLowerCase()) {
                                case "thunder":
                                    if (!pLevel.isThundering()) {
                                        return false;
                                    }
                                    break;
                                case "rain":
                                    if (!pLevel.isRaining()) {
                                        return false;
                                    }
                                    break;
                                case "clear":
                                    if (pLevel.isRaining() || pLevel.isThundering()) {
                                        return false;
                                    }
                                    break;
                            }
                        }
                        
                        // 检查时间要求
                        if (finalTimeOfDay != null) {
                            int skyDarken = pLevel.getSkyDarken();
                            switch (finalTimeOfDay.toLowerCase()) {
                                case "night":
                                    // getSkyDarken >= 4 表示夜晚
                                    if (skyDarken < 4) {
                                        return false;
                                    }
                                    break;
                                case "day":
                                    // getSkyDarken < 4 表示白天
                                    if (skyDarken >= 4) {
                                        return false;
                                    }
                                    break;
                            }
                        }
                        
                        // 检查生物群系要求
                        if (finalBiome != null || (finalBiomeType != null && "coldEnoughToSnow".equals(finalBiomeType))) {
                            String type = finalBiomeType != null ? finalBiomeType.toLowerCase() : "id";
                            var currentBiome = pLevel.getBiome(pPos);
                            
                            if ("coldenoughtosnow".equals(type)) {
                                // 寒冷检查：coldEnoughToSnow
                                if (!currentBiome.get().coldEnoughToSnow(pPos)) {
                                    return false;
                                }
                            } else if ("tags".equals(type)) {
                                // 标签检查
                                boolean biomeTagMatch = false;
                                
                                if (finalBiome instanceof CharSequence) {
                                    String tagStr = finalBiome.toString().replace("#", "");
                                    var tagKey = TagKey.create(Registries.BIOME, ResourceLocation.tryParse(tagStr));
                                    if (tagKey != null && currentBiome.is(tagKey)) {
                                        biomeTagMatch = true;
                                    }
                                } else if (finalBiome instanceof List || (finalBiome != null && finalBiome.getClass().isArray())) {
                                    List<?> list = ListJS.of(finalBiome);
                                    if (list != null) {
                                        for (Object tag : list) {
                                            if (tag instanceof CharSequence) {
                                                String tagStr = tag.toString().replace("#", "");
                                                var tagKey = TagKey.create(Registries.BIOME, ResourceLocation.tryParse(tagStr));
                                                if (tagKey != null && currentBiome.is(tagKey)) {
                                                    biomeTagMatch = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (!biomeTagMatch) {
                                    return false;
                                }
                            } else {
                                // ID 检查（默认）
                                boolean biomeIdMatch = false;
                                var biomeRegistry = pLevel.registryAccess().registry(Registries.BIOME);
                                
                                if (biomeRegistry != null) {
                                    if (finalBiome instanceof CharSequence) {
                                        // 单个ID
                                        var biomeKey = ResourceLocation.tryParse(finalBiome.toString());
                                        if (biomeKey != null) {
                                            var biomeHolder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
                                            if (biomeHolder.isPresent() && pLevel.getBiome(pPos).equals(biomeHolder.get())) {
                                                biomeIdMatch = true;
                                            }
                                        }
                                    } else if (finalBiome instanceof List || (finalBiome != null && finalBiome.getClass().isArray())) {
                                        // 多个ID（OR 逻辑，满足任意一个即可）
                                        List<?> list = ListJS.of(finalBiome);
                                        if (list != null) {
                                            for (Object biomeId : list) {
                                                if (biomeId instanceof CharSequence) {
                                                    var biomeKey = ResourceLocation.tryParse(biomeId.toString());
                                                    if (biomeKey != null) {
                                                        var biomeHolder = biomeRegistry.getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
                                                        if (biomeHolder.isPresent() && pLevel.getBiome(pPos).equals(biomeHolder.get())) {
                                                            biomeIdMatch = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (!biomeIdMatch) {
                                    return false;
                                }
                            }
                        }
                        
                        // 检查高度要求
                        if (finalMinY != null && pPos.getY() < finalMinY) {
                            return false;
                        }
                        if (finalMaxY != null && pPos.getY() > finalMaxY) {
                            return false;
                        }
                        
                        // 检查天空可见性要求
                        if (finalRequireSkyVisible != null) {
                            boolean canSeeSky = pLevel.canSeeSky(pPos.above());
                            if (finalRequireSkyVisible && !canSeeSky) {
                                return false;
                            }
                            if (!finalRequireSkyVisible && canSeeSky) {
                                return false;
                            }
                        }
                        
                        // 如果有配置的方块需求，使用配置
                        if (!blockRequirements.isEmpty()) {
                            Map<SizedIngredient, Integer> found = new HashMap<>();
                            
                            // 扫描范围内的方块
                            for (int i = -finalRange; i <= finalRange; i++) {
                                for (int j = -finalRange; j <= finalRange; j++) {
                                    for (int k = -finalRange; k <= finalRange; k++) {
                                        BlockPos blockPos = pPos.offset(i, j, k);
                                        BlockState blockState = pLevel.getBlockState(blockPos);
                                        
                                        // 检查每个需求
                                        for (Map.Entry<SizedIngredient, Integer> entry : blockRequirements.entrySet()) {
                                            SizedIngredient sizedIngredient = entry.getKey();
                                            // 获取方块对应的物品栈
                                            var item = blockState.getBlock().asItem();
                                            if (item != null && !item.isEmpty()) {
                                                // 使用 SizedIngredient 的 ingredient 来测试
                                                if (sizedIngredient.ingredient().test(item.getDefaultInstance())) {
                                                    found.put(sizedIngredient, found.getOrDefault(sizedIngredient, 0) + 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 验证是否满足所有需求
                            for (Map.Entry<SizedIngredient, Integer> entry : blockRequirements.entrySet()) {
                                int required = entry.getValue();
                                int actual = found.getOrDefault(entry.getKey(), 0);
                                if (actual < required) {
                                    return false;
                                }
                            }
                            
                            return true;
                        }
                        
                        // 如果没有配置方块需求，使用原始检查逻辑
                        return originalRitual.getRequirement(pTileEntity, pPos, pLevel);
                    } catch (Exception e) {
                        System.err.println("[Goety仪式] 执行仪式检查时出错: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            };
        }
    }
}
