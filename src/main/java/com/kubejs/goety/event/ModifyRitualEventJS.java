package com.kubejs.goety.event;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.api.ritual.RitualType;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.item.InputItem;
import net.minecraft.world.item.crafting.Ingredient;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.typings.Generics;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.kubejs.goety.util.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

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
    @Generics(RitualModifierImpl.class)
    public void modify(String ritualId, Consumer<RitualModifierImpl>... modifiers) {
        if (ritualId == null || ritualId.isEmpty()) {
            ScriptType.SERVER.console.error("Ritual ID cannot be empty");
            return;
        }
        
        if (modifiers == null || modifiers.length == 0) {
            ScriptType.SERVER.console.error("At least one modifier function is required");
            return;
        }
        
        IRitualType existingRitual = RitualType.getRitualType(ritualId);
        if (existingRitual == null) {
            ScriptType.SERVER.console.error("Ritual type '" + ritualId + "' does not exist, cannot modify");
            return;
        }
        
        try {
            // 统一处理所有修改器函数（单个或多个），创建条件组（OR 逻辑）
            List<RitualModifierImpl> conditionGroups = new ArrayList<>();
            
            for (int i = 0; i < modifiers.length; i++) {
                Consumer<RitualModifierImpl> modifier = modifiers[i];
                if (modifier != null) {
                    RitualModifierImpl group = new RitualModifierImpl(ritualId, existingRitual, this);
                    // KubeJS 会自动将 JavaScript 函数转换为 Consumer
                    modifier.accept(group);
                    conditionGroups.add(group);
                }
            }
            
            if (!conditionGroups.isEmpty()) {
                // 使用反射访问 RitualType 的内部 Map 来注册修改后的仪式
                IRitualType modifiedRitual = buildRitualTypeWithOR(ritualId, conditionGroups, existingRitual);
                try {
                    java.lang.reflect.Field field = RitualType.class.getDeclaredField("RITUAL_TYPE_LIST");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, IRitualType> ritualMap = (java.util.Map<String, IRitualType>) field.get(null);
                    ritualMap.put(ritualId, modifiedRitual);
                } catch (NoSuchFieldException e) {
                    // 如果字段不存在，尝试其他可能的字段名
                    try {
                        java.lang.reflect.Field[] fields = RitualType.class.getDeclaredFields();
                        for (java.lang.reflect.Field f : fields) {
                            if (java.util.Map.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, IRitualType> ritualMap = (java.util.Map<String, IRitualType>) f.get(null);
                                if (ritualMap != null) {
                                    ritualMap.put(ritualId, modifiedRitual);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e2) {
                        ScriptType.SERVER.console.error("Failed to modify ritual type '" + ritualId + "': " + e2.getMessage());
                        e2.printStackTrace();
                    }
                } catch (Exception e) {
                    ScriptType.SERVER.console.error("Failed to modify ritual type '" + ritualId + "': " + e.getMessage());
                    e.printStackTrace();
                }
                if (conditionGroups.size() == 1) {
                    ScriptType.SERVER.console.info("✓ Modified ritual type: " + ritualId);
                } else {
                    ScriptType.SERVER.console.info("✓ Modified ritual type: " + ritualId + " (" + conditionGroups.size() + " condition groups, OR logic)");
                }
            }
        } catch (Exception e) {
            ScriptType.SERVER.console.error("Error modifying ritual type '" + ritualId + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 构建支持 OR 逻辑的 IRitualType
     * 多个条件组之间使用 OR 连接，任意一个满足即可
     */
    private IRitualType buildRitualTypeWithOR(String ritualId, List<RitualModifierImpl> conditionGroups, IRitualType originalRitual) {
        final List<RitualModifierImpl> finalGroups = new ArrayList<>(conditionGroups);
        final String finalRitualId = ritualId;
        
        // 解析 JEI 图标（从第一个设置了图标的条件组中获取）
        net.minecraft.world.item.ItemStack finalJeiIcon = originalRitual != null ? originalRitual.getJeiIcon() : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OBSIDIAN);
        for (RitualModifierImpl group : conditionGroups) {
            if (group.jeiIcon != null) {
                try {
                    ScriptType.SERVER.console.info("Parsing JEI icon: " + group.jeiIcon);
                    InputItem inputItem = InputItem.of(group.jeiIcon);
                    if (inputItem != null && !inputItem.isEmpty()) {
                        net.minecraft.world.item.ItemStack[] items = inputItem.ingredient.getItems();
                        if (items.length > 0) {
                            finalJeiIcon = items[0];
                            ScriptType.SERVER.console.info("✓ JEI icon set to: " + finalJeiIcon.getItem().toString());
                            break;  // 使用第一个找到的图标
                        } else {
                            ScriptType.SERVER.console.warn("JEI icon ingredient has no items: " + group.jeiIcon);
                        }
                    } else {
                        ScriptType.SERVER.console.warn("JEI icon InputItem is null or empty: " + group.jeiIcon);
                    }
                } catch (Exception e) {
                    ScriptType.SERVER.console.error("Failed to parse JEI icon: " + group.jeiIcon + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // 获取 onFinish 回调（从第一个设置了回调的条件组中获取）
        final Object finalOnFinish = conditionGroups.stream()
            .filter(group -> group.onFinish != null)
            .findFirst()
            .map(group -> group.onFinish)
            .orElse(null);
        
        final net.minecraft.world.item.ItemStack finalJeiIconFinal = finalJeiIcon;
        
        return new IRitualType() {
            @Override
            public String getName() {
                return finalRitualId;  // 使用ritualId作为名称，不再使用条件组内部的name
            }
            
            @Override
            public net.minecraft.world.item.ItemStack getJeiIcon() {
                return finalJeiIconFinal;
            }
            
            @Override
            public void onFinishRitual(Level world, BlockPos darkAltarPos, 
                                     com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity tileEntity,
                                     net.minecraft.world.entity.player.Player castingPlayer, 
                                     net.minecraft.world.item.ItemStack activationItem) {
                if (finalOnFinish instanceof RegisterRitualEventJS.OnFinishCallback) {
                    // 如果是 OnFinishCallback 接口，直接调用
                    try {
                        ((RegisterRitualEventJS.OnFinishCallback) finalOnFinish).accept(world, darkAltarPos, tileEntity, castingPlayer, activationItem);
                    } catch (Exception e) {
                        ScriptType.SERVER.console.error("Error executing onFinishRitual callback: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (finalOnFinish instanceof dev.latvian.mods.rhino.BaseFunction) {
                    // 如果是 JavaScript 函数，通过 Rhino 调用
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
                } else if (originalRitual != null) {
                    // 如果没有设置自定义回调，使用原始仪式的回调
                    originalRitual.onFinishRitual(world, darkAltarPos, tileEntity, castingPlayer, activationItem);
                }
            }
            
            @Override
            public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity,
                                        BlockPos pPos,
                                        Level pLevel) {
                return getRequirement(pTileEntity, null, pPos, pLevel);
            }

            @Override
            public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity,
                                        @Nullable Player pPlayer,
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
            if (group.biome != null || group.biomeType != null) {
                String type = group.biomeType != null ? group.biomeType.toLowerCase() : "id";
                var currentBiome = pLevel.getBiome(pPos);
                
                if ("func".equals(type)) {
                    // 方法检查：使用反射调用 Biome 类的方法
                    if (group.biome == null) {
                        ScriptType.SERVER.console.warn("Biome method name is null for 'func' type");
                        return false;
                    }
                    
                    String methodName = group.biome.toString();
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
                                var biomeHolder = biomeRegistry.get().getHolder(ResourceKey.create(Registries.BIOME, biomeKey));
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
            
            // 检查祭坛含水要求
            if (group.requireAltarWaterlogged != null) {
                var altarState = pTileEntity.getBlockState();
                var BlockStateProperties = net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
                boolean isWaterlogged = altarState.hasProperty(BlockStateProperties) && altarState.getValue(BlockStateProperties);
                if (group.requireAltarWaterlogged && !isWaterlogged) {
                    return false;
                }
                if (!group.requireAltarWaterlogged && isWaterlogged) {
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
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
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
            System.err.println("[Goety Ritual] Error checking condition group: " + e.getMessage());
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
        private final IRitualType originalRitual;
        private final ModifyRitualEventJS event;
        
        public RitualModifierImpl(String ritualId, IRitualType originalRitual, ModifyRitualEventJS event) {
            this.ritualId = ritualId;
            this.originalRitual = originalRitual;
            this.event = event;
            this.name = originalRitual.getName();
            this.range = 16;  // 默认范围
            this.blocks = null;
            this.dimension = null;
            this.dimensionContainsMatch = false;  // 默认精确匹配
            this.customRequirement = null;
            this.jeiIcon = null;  // 默认使用原始仪式的图标
            this.onFinish = null;  // 默认使用原始仪式的回调
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
         * 支持格式：'9x minecraft:stone' 或 ['9x minecraft:stone', '3x minecraft:diamond_block']
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
         * @param biome 生物群系值（可以是字符串或数组），或方法名（当 type='func' 时）
         * @param type 生物群系检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'func'（方法调用）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置生物群系要求", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "biome", value = "生物群系值（可以是字符串或数组），或方法名（当 type='func' 时）"),
            @dev.latvian.mods.kubejs.typings.Param(name = "type", value = "检查类型：'id'（生物群系ID，默认）、'tags'（标签）、'func'（方法调用）")
        })
        public RitualModifierImpl setBiome(Object biome, String type) {
            this.biome = biome;
            this.biomeType = (type != null && !type.isEmpty()) ? type : "id";
            return this;
        }
        
        /**
         * 设置生物群系要求（默认为 ID 类型）
         * 如果需要调用 Biome 方法，请使用 setBiome(biome, 'func')
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
         * 设置是否需要祭坛含水
         * @param requireAltarWaterlogged true（需要祭坛含水）、false（需要祭坛不含水）、null（不检查）
         */
        @dev.latvian.mods.kubejs.typings.Info(value = "设置是否需要祭坛含水", params = {
            @dev.latvian.mods.kubejs.typings.Param(name = "requireAltarWaterlogged", value = "true（需要祭坛含水）、false（需要祭坛不含水）、null（不检查）")
        })
        public RitualModifierImpl setRequireAltarWaterlogged(Boolean requireAltarWaterlogged) {
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
        public RitualModifierImpl setJeiIcon(Object icon) {
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
        @Generics({net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class, 
                   com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity.class, 
                   net.minecraft.world.entity.player.Player.class, net.minecraft.world.item.ItemStack.class})
        public RitualModifierImpl setOnFinish(RegisterRitualEventJS.OnFinishCallback callback) {
            this.onFinish = callback;
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
            final Boolean finalRequireAltarWaterlogged = this.requireAltarWaterlogged;
            final Object finalOnFinish = this.onFinish;
            
            // 解析 JEI 图标
            net.minecraft.world.item.ItemStack tempJeiIcon = originalRitual.getJeiIcon();
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
                    if (finalOnFinish instanceof RegisterRitualEventJS.OnFinishCallback) {
                        // 如果是 OnFinishCallback 接口，直接调用
                        try {
                            ((RegisterRitualEventJS.OnFinishCallback) finalOnFinish).accept(world, darkAltarPos, tileEntity, castingPlayer, activationItem);
                        } catch (Exception e) {
                            ScriptType.SERVER.console.error("Error executing onFinishRitual callback: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else if (finalOnFinish instanceof dev.latvian.mods.rhino.BaseFunction) {
                        // 如果是 JavaScript 函数，通过 Rhino 调用
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
                    } else {
                        // 如果没有设置自定义回调，使用原始仪式的回调
                        originalRitual.onFinishRitual(world, darkAltarPos, tileEntity, castingPlayer, activationItem);
                    }
                }
                
                @Override
                public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity, 
                                            BlockPos pPos, 
                                            Level pLevel) {
                    return getRequirement(pTileEntity, null, pPos, pLevel);
                }

                @Override
                public boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity pTileEntity,
                                            @Nullable Player pPlayer,
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
                        
                        // 如果有配置的方块需求，使用配置
                        if (!blockRequirements.isEmpty()) {
                            Map<SizedIngredient, Integer> found = new HashMap<>();
                            
                            // 扫描范围内的方块
                            for (int i = -finalRange; i <= finalRange; i++) {
                                for (int j = -finalRange; j <= finalRange; j++) {
                                    for (int k = -finalRange; k <= finalRange; k++) {
                                        BlockPos blockPos = pPos.offset(i, j, k);
                                        BlockState blockState = pLevel.getBlockState(blockPos);
                                        
                                        // 获取方块对应的物品栈
                                        var item = blockState.getBlock().asItem();
                                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                            net.minecraft.world.item.ItemStack itemStack = item.getDefaultInstance();
                                            
                                            // 检查这个方块是否匹配某个需求（只匹配第一个匹配的需求）
                                            for (Map.Entry<SizedIngredient, Integer> entry : blockRequirements.entrySet()) {
                                                SizedIngredient sizedIngredient = entry.getKey();
                                                // 使用 SizedIngredient 的 ingredient 来测试
                                                if (sizedIngredient.ingredient().test(itemStack)) {
                                                    found.put(sizedIngredient, found.getOrDefault(sizedIngredient, 0) + 1);
                                                    break;  // 只计数一次，匹配第一个需求后退出
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
                                    ScriptType.SERVER.console.debug("Ritual check failed: Required " + required + " blocks, found " + actual);
                                    return false;
                                }
                            }
                            
                            return true;
                        }
                        
                        // 如果没有配置方块需求，使用原始检查逻辑
                        return originalRitual.getRequirement(pTileEntity, pPlayer, pPos, pLevel);
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
