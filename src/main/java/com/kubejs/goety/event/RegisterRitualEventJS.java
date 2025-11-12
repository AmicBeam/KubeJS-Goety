package com.kubejs.goety.event;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.api.ritual.RitualType;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.UtilsJS;
import dev.latvian.mods.kubejs.script.ScriptManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.Ingredient;
import com.kubejs.goety.util.SizedIngredient;
import dev.latvian.mods.kubejs.script.ScriptType;

import java.util.*;

/**
 * 注册新仪式的事件
 * 
 * 在脚本中使用：
 * GoetyEvents.registerRitual(event => {
 *     event.create('my_custom_ritual', ritual => {
 *         ritual.name = 'my_custom_ritual';
 *         ritual.range = 16;  // 扫描范围
 *         ritual.blocks = ['9x minecraft:stone', '3x minecraft:diamond_block'];  // 方块需求
 *         ritual.dimension = 'minecraft:overworld';  // 可选：维度限制
 *     });
 * });
 */
@Info("用于注册新的仪式类型")
public class RegisterRitualEventJS extends EventJS {
    
    /**
     * 创建新的仪式类型
     * 
     * @param ritualId 仪式 ID（字符串，如 'my_custom_ritual'）
     * @param builder 构建器对象或函数，接收一个仪式对象用于配置
     */
    @Info(value = "创建一个新的仪式类型", params = {
        @Param(name = "ritualId", value = "仪式的唯一标识符（字符串）"),
        @Param(name = "builder", value = "构建器函数，用于配置仪式属性")
    })
    public void create(String ritualId, Object builder) {
        if (ritualId == null || ritualId.isEmpty()) {
            ScriptType.SERVER.console.error("Ritual ID cannot be empty");
            return;
        }
        
        if (RitualType.getRitualTypeList().containsKey(ritualId)) {
            ScriptType.SERVER.console.warn("Ritual type '" + ritualId + "' already exists, will be overwritten");
        }
        
        try {
            RitualBuilderImpl ritualBuilder = new RitualBuilderImpl(ritualId, this);
            
            // 如果 builder 是函数，调用它
            if (builder instanceof dev.latvian.mods.rhino.BaseFunction) {
                var cx = ScriptManager.getCurrentContext();
                if (cx != null) {
                    var scope = ScriptType.SERVER.manager.get().topLevelScope;
                    ((dev.latvian.mods.rhino.BaseFunction) builder).call(
                        cx,
                        scope,
                        scope,
                        new Object[]{ritualBuilder}
                    );
                }
            } else {
                ScriptType.SERVER.console.error("Builder must be a function");
                return;
            }
            
            if (ritualBuilder.name == null || ritualBuilder.name.isEmpty()) {
                ritualBuilder.name = ritualId;
            }
            
            RitualType.addRitualType(ritualId, ritualBuilder.buildRitualType());
            ScriptType.SERVER.console.info("✓ Registered new ritual type: " + ritualId);
        } catch (Exception e) {
            ScriptType.SERVER.console.error("Error registering ritual type '" + ritualId + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 仪式构建器实现
     */
    public static class RitualBuilderImpl {
        public String name;
        public Integer range;
        public Object blocks;  // 可以是字符串、数组或对象
        public String dimension;  // 可选：维度ID
        public Boolean dimensionContainsMatch;  // 可选：维度匹配模式，false（精确匹配，默认）或 true（模糊匹配）
        public Object customRequirement;  // 可选：自定义检查函数
        public Object jeiIcon;  // JEI 图标物品
        public Object onFinish;  // 仪式完成时的回调函数
        
        // 新增配置选项
        public String weather;  // 天气要求：'thunder', 'rain', 'clear', null（不检查）
        public String timeOfDay;  // 时间要求：'day', 'night', null（不检查）
        public Object biome;  // 生物群系要求（可以是字符串或数组）
        public String biomeType;  // 生物群系检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'func'（方法调用）
        public Integer minY;  // 最小高度要求
        public Integer maxY;  // 最大高度要求
        public Boolean requireSkyVisible;  // 是否需要看到天空（true/false/null）
        public Boolean requireAltarWaterlogged;  // 是否需要祭坛含水（true/false/null）
        
        private final String ritualId;
        private final RegisterRitualEventJS event;
        
        public RitualBuilderImpl(String ritualId, RegisterRitualEventJS event) {
            this.ritualId = ritualId;
            this.event = event;
            this.name = ritualId;
            this.range = 16;  // 默认范围
            this.blocks = null;
            this.dimension = null;
            this.dimensionContainsMatch = false;  // 默认精确匹配
            this.customRequirement = null;
            this.jeiIcon = null;  // 默认使用黑曜石
            this.onFinish = null;  // 默认无回调
            this.weather = null;
            this.timeOfDay = null;
            this.biome = null;
            this.biomeType = "id";  // 默认ID类型
            this.minY = null;
            this.maxY = null;
            this.requireSkyVisible = null;
            this.requireAltarWaterlogged = null;
        }
        
        /**
         * 设置仪式名称
         */
        public RitualBuilderImpl setName(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * 设置扫描范围
         */
        public RitualBuilderImpl setRange(Integer range) {
            this.range = range;
            return this;
        }
        
        /**
         * 设置方块需求
         * 支持格式：'9x minecraft:stone' 或 ['9x minecraft:stone', '3x minecraft:diamond_block']
         */
        public RitualBuilderImpl setBlocks(Object blocks) {
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
        public RitualBuilderImpl setDimension(String dimension, Boolean containsMatch) {
            this.dimension = dimension;
            this.dimensionContainsMatch = (containsMatch != null) ? containsMatch : false;
            return this;
        }
        
        /**
         * 设置维度限制（精确匹配，默认）
         */
        public RitualBuilderImpl setDimension(String dimension) {
            return setDimension(dimension, false);
        }
        
        /**
         * 设置自定义检查函数（可选，如果设置了会覆盖 blocks 检查）
         */
        public RitualBuilderImpl setRequirement(Object requirement) {
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
        public RitualBuilderImpl setWeather(String weather) {
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
        public RitualBuilderImpl setTimeOfDay(String timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }
        
        /**
         * 设置生物群系要求
         * @param biome 生物群系值（可以是字符串或数组），或方法名（当 type='func' 时）
         * @param type 生物群系检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'func'（方法调用）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置生物群系要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "biome", value = "生物群系值（可以是字符串或数组），或方法名（当 type='func' 时）"),
            @dev.latvian.mods.kubejs.typings.Param(name = "type", value = "检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'func'（方法调用）")
        })
        public RitualBuilderImpl setBiome(Object biome, String type) {
            this.biome = biome;
            this.biomeType = (type != null && !type.isEmpty()) ? type : "id";
            return this;
        }
        
        /**
         * 设置生物群系要求（默认ID类型）
         */
        public RitualBuilderImpl setBiome(Object biome) {
            return setBiome(biome, "id");
        }
        
        /**
         * 设置最小高度要求
         * @param minY 最小Y坐标
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置最小高度要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "minY", value = "最小Y坐标")
        })
        public RitualBuilderImpl setMinY(Integer minY) {
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
        public RitualBuilderImpl setMaxY(Integer maxY) {
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
        public RitualBuilderImpl setRequireSkyVisible(Boolean requireSkyVisible) {
            this.requireSkyVisible = requireSkyVisible;
            return this;
        }
        
        /**
         * 设置是否需要祭坛含水
         * @param requireAltarWaterlogged true（需要祭坛含水）、false（需要祭坛不含水）、null（不检查）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置是否需要祭坛含水", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "requireAltarWaterlogged", value = "true（需要祭坛含水）、false（需要祭坛不含水）、null（不检查）")
        })
        public RitualBuilderImpl setRequireAltarWaterlogged(Boolean requireAltarWaterlogged) {
            this.requireAltarWaterlogged = requireAltarWaterlogged;
            return this;
        }
        
        /**
         * 设置 JEI 显示的图标
         * @param icon 物品ID（字符串）或物品对象
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置 JEI 显示的图标", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "icon", value = "物品ID（字符串）或物品对象")
        })
        public RitualBuilderImpl setJeiIcon(Object icon) {
            this.jeiIcon = icon;
            return this;
        }
        
        /**
         * 设置仪式完成时的回调函数
         * @param callback 回调函数，接收参数：(world, darkAltarPos, tileEntity, castingPlayer, activationItem)
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置仪式完成时的回调函数", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "callback", value = "回调函数，接收参数：(world, darkAltarPos, tileEntity, castingPlayer, activationItem)")
        })
        public RitualBuilderImpl setOnFinish(Object callback) {
            this.onFinish = callback;
            return this;
        }
        
        /**
         * 解析方块需求配置
         * 支持：
         * - 字符串：'minecraft:stone' 或 '9x minecraft:stone'
         * - ItemStack 对象：Item.of('minecraft:stone')，包括带 NBT 的
         * - 数组：混合使用以上格式
         */
        private Map<SizedIngredient, Integer> parseBlocks(Object blocksConfig) {
            Map<SizedIngredient, Integer> requirements = new LinkedHashMap<>();
            
            if (blocksConfig == null) {
                return requirements;
            }
            
            var cx = ScriptManager.getCurrentContext();
            
            // 如果是字符串，解析为单个需求
            if (blocksConfig instanceof CharSequence) {
                try {
                    InputItem inputItem = InputItem.of(blocksConfig.toString());
                    if (inputItem != null && !inputItem.isEmpty()) {
                        Ingredient ingredient = inputItem.ingredient;
                        int count = inputItem.count;
                        SizedIngredient sizedIngredient = SizedIngredient.of(ingredient, count);
                        requirements.put(sizedIngredient, count);
                    }
                } catch (Exception e) {
                            ScriptType.SERVER.console.error("Failed to parse block requirement: " + blocksConfig + " - " + e.getMessage());
                }
            }
            // 如果是数组
            else if (blocksConfig instanceof List || blocksConfig.getClass().isArray()) {
                List<?> list = ListJS.of(blocksConfig);
                if (list != null) {
                    for (Object item : list) {
                        try {
                            // 支持字符串和 ItemStack 对象（包括带 NBT 的）
                            InputItem inputItem = InputItem.of(item);
                            if (inputItem != null && !inputItem.isEmpty()) {
                                Ingredient ingredient = inputItem.ingredient;
                                int count = inputItem.count;
                                SizedIngredient sizedIngredient = SizedIngredient.of(ingredient, count);
                                requirements.put(sizedIngredient, count);
                            }
                        } catch (Exception e) {
                            ScriptType.SERVER.console.error("Failed to parse block requirement: " + item + " - " + e.getMessage());
                        }
                    }
                }
            }
            
            return requirements;
        }
        
        /**
         * 构建 IRitualType 实例
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
            final Boolean finalRequireAltarWaterlogged = this.requireAltarWaterlogged;
            final Object finalOnFinish = this.onFinish;
            
            // 解析 JEI 图标
            net.minecraft.world.item.ItemStack tempJeiIcon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OBSIDIAN);
            if (this.jeiIcon != null) {
                try {
                    InputItem inputItem = InputItem.of(this.jeiIcon);
                    if (inputItem != null && !inputItem.isEmpty()) {
                        net.minecraft.world.item.ItemStack[] items = inputItem.ingredient.getItems();
                        if (items.length > 0) {
                            tempJeiIcon = items[0];
                        }
                    }
                } catch (Exception e) {
                    ScriptType.SERVER.console.error("Failed to parse JEI icon: " + this.jeiIcon + " - " + e.getMessage());
                }
            }
            final net.minecraft.world.item.ItemStack finalJeiIcon = tempJeiIcon;
            
            return new IRitualType() {
                @Override
                public String getName() {
                    return finalName;
                }
                
                @Override
                public net.minecraft.world.item.ItemStack getJeiIcon() {
                    return finalJeiIcon;
                }
                
                @Override
                public void onFinishRitual(Level world, BlockPos darkAltarPos, 
                                         com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity tileEntity,
                                         net.minecraft.world.entity.player.Player castingPlayer, 
                                         net.minecraft.world.item.ItemStack activationItem) {
                    if (finalOnFinish instanceof dev.latvian.mods.rhino.BaseFunction) {
                        dev.latvian.mods.rhino.BaseFunction func = (dev.latvian.mods.rhino.BaseFunction) finalOnFinish;
                        var cx = ScriptManager.getCurrentContext();
                        if (cx != null) {
                            var scope = ScriptType.SERVER.manager.get().topLevelScope;
                            try {
                                func.call(
                                    cx,
                                    scope,
                                    scope,
                                    new Object[]{world, darkAltarPos, tileEntity, castingPlayer, activationItem}
                                );
                            } catch (Exception e) {
                                ScriptType.SERVER.console.error("Error executing onFinishRitual callback: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
                
                @Override
                public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity, 
                                            BlockPos pPos, 
                                            Level pLevel) {
                    try {
                        // 如果有自定义检查函数，优先使用
                        if (finalCustomRequirement instanceof dev.latvian.mods.rhino.BaseFunction) {
                            dev.latvian.mods.rhino.BaseFunction func = (dev.latvian.mods.rhino.BaseFunction) finalCustomRequirement;
                            var cx = ScriptManager.getCurrentContext();
                            if (cx != null) {
                                var scope = ScriptType.SERVER.manager.get().topLevelScope;
                                Object result = func.call(
                                    cx,
                                    scope,
                                    scope,
                                    new Object[]{pTileEntity, pPos, pLevel}
                                );
                                return UtilsJS.cast(result);
                            }
                        }
                        
                        // 检查维度限制
                        if (finalDimension != null && !finalDimension.isEmpty()) {
                            ResourceKey<Level> dimensionKey = pLevel.dimension();
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
                        if (finalBiome != null || finalBiomeType != null) {
                            String type = finalBiomeType != null ? finalBiomeType.toLowerCase() : "id";
                            var currentBiome = pLevel.getBiome(pPos);
                            
                            if ("func".equals(type)) {
                                // 方法检查：使用反射调用 Biome 类的方法
                                if (finalBiome == null) {
                                    ScriptType.SERVER.console.warn("Biome method name is null for 'func' type");
                                    return false;
                                }
                                
                                String methodName = finalBiome.toString();
                                try {
                                    var biome = currentBiome.get();
                                    var biomeClass = biome.getClass();
                                    
                                    // 尝试不带参数的方法
                                    try {
                                        var method = biomeClass.getMethod(methodName);
                                        Object result = method.invoke(biome);
                                        if (result instanceof Boolean && !(Boolean) result) {
                                            return false;
                                        }
                                    } catch (NoSuchMethodException e1) {
                                        // 尝试带 BlockPos 参数的方法
                                        try {
                                            var method = biomeClass.getMethod(methodName, net.minecraft.core.BlockPos.class);
                                            Object result = method.invoke(biome, pPos);
                                            if (result instanceof Boolean && !(Boolean) result) {
                                                return false;
                                            }
                                        } catch (NoSuchMethodException e2) {
                                            // 方法不存在，记录警告
                                            ScriptType.SERVER.console.warn("Biome method '" + methodName + "' not found for biome check in ritual");
                                            return false;
                                        }
                                    }
                                } catch (Exception e) {
                                    ScriptType.SERVER.console.error("Error invoking biome method '" + methodName + "': " + e.getMessage());
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
                                            var biomeHolder = biomeRegistry.get().getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
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
                                                        var biomeHolder = biomeRegistry.get().getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
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
                        
                        // 检查祭坛含水要求
                        if (finalRequireAltarWaterlogged != null) {
                            var altarState = pTileEntity.getBlockState();
                            var BlockStateProperties = net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
                            boolean isWaterlogged = altarState.hasProperty(BlockStateProperties) && altarState.getValue(BlockStateProperties);
                            if (finalRequireAltarWaterlogged && !isWaterlogged) {
                                return false;
                            }
                            if (!finalRequireAltarWaterlogged && isWaterlogged) {
                                return false;
                            }
                        }
                        
                        // 检查方块需求
                        if (blockRequirements.isEmpty()) {
                            return true;  // 没有方块需求，默认通过
                        }
                        
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
                                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
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
                    } catch (Exception e) {
                        System.err.println("[Goety Ritual] Error executing ritual check: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            };
        }
    }
}
