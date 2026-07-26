package com.kubejs.goety.event;

import com.Polarice3.Goety.common.effects.brew.BrewEffect;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.Polarice3.Goety.common.effects.brew.PotionBrewEffect;
import com.Polarice3.Goety.common.effects.brew.modifiers.BrewModifier;
import com.Polarice3.Goety.common.effects.brew.modifiers.CapacityModifier;
import com.kubejs.goety.brew.BrewData;
import com.kubejs.goety.bridge.BrewEffectsInvoker;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 注册药酿配置的事件
 * 
 * 在脚本中使用：
 * GoetyEvents.registerBrew(event => {
 *     event.addCapacity('minecraft:nether_wart', 0);
 *     event.setCapacityLevels([2,2,2,2,4]);
 *     event.addAugmentation('minecraft:redstone', 'duration', 0);
 * });
 */
@Info("用于注册药酿系统的配置（容量剂、催化剂、增强剂）")
public class RegisterBrewEventJS extends EventJS {
    
    private static Method modifierRegisterMethod;
    private static java.lang.reflect.Field modifiersField;
    
    static {
        try {
            modifierRegisterMethod = BrewEffects.class.getDeclaredMethod("modifierRegister", BrewModifier.class, Item.class);
            modifierRegisterMethod.setAccessible(true);
            
            modifiersField = BrewEffects.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[KubeJS Goety] Failed to initialize reflection methods: " + e.getMessage());
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
        @Param(name = "level", value = "等级（>=0）")
    })
    public void addCapacity(Object item, int level) {
        if (level < 0) {
            ScriptType.SERVER.console.error("容量剂等级必须 >= 0，当前值: " + level);
            return;
        }
        
        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        try {
            CapacityModifier modifier = new CapacityModifier(level);
            if (BrewEffects.INSTANCE instanceof BrewEffectsInvoker invoker) {
                invoker.forceModifierRegister_(modifier, itemObj);
            } else {
                modifierRegisterMethod.invoke(BrewEffects.INSTANCE, modifier, itemObj);
            }
            ScriptType.SERVER.console.info("✓ 已注册容量剂: " + itemObj + " (等级: " + level + ")");
        } catch (Exception e) {
            ScriptType.SERVER.console.error("注册容量剂失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Info(value = "设置容量等级增量表", params = {
        @Param(name = "levels", value = "等级增量数组，从1级开始，例如 [2,2,2,2,4]")
    })
    public void setCapacityLevels(Object levels) {
        List<?> list = ListJS.of(levels);
        if (list == null) {
            ScriptType.SERVER.console.error("容量等级增量表不能为空");
            return;
        }
        List<Integer> deltas = new ArrayList<>();
        for (Object item : list) {
            int value = ((Number) UtilsJS.cast(item)).intValue();
            if (value < 0) {
                ScriptType.SERVER.console.error("容量增量不能为负数: " + value);
                return;
            }
            deltas.add(value);
        }
        com.kubejs.goety.brew.BrewData.setCapacityLevelDeltas(deltas);
        ScriptType.SERVER.console.info("✓ 已设置容量等级增量表: " + deltas);
    }

    @Info(value = "设置等级型增强剂的等级表", params = {
        @Param(name = "modifier", value = "增强类型：duration, amplifier, aoe, linger, quaff, velocity"),
        @Param(name = "levels", value = "等级表，从0级开始。可用数字数组 [1,1,1] 或对象数组 [{value:1,cost:2.0}]")
    })
    public void setAugmentationLevels(String modifier, Object levels) {
        if (modifier == null || modifier.isEmpty()) {
            ScriptType.SERVER.console.error("增强类型不能为空");
            return;
        }

        String modifierLower = modifier.toLowerCase();
        if (!BrewData.isLevelableAugmentation(modifierLower)) {
            ScriptType.SERVER.console.error("只有等级型增强剂支持等级表: duration, amplifier, aoe, linger, quaff, velocity");
            return;
        }

        List<?> list = ListJS.of(levels);
        if (list == null || list.isEmpty()) {
            ScriptType.SERVER.console.error("增强剂等级表不能为空");
            return;
        }

        List<BrewData.AugmentationLevel> parsedLevels = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                parsedLevels.add(parseAugmentationLevel(modifierLower, list.get(i), i));
            } catch (IllegalArgumentException e) {
                ScriptType.SERVER.console.error(e.getMessage());
                return;
            }
        }

        BrewData.setAugmentationLevels(modifierLower, parsedLevels);
        ScriptType.SERVER.console.info("✓ 已设置增强剂等级表: " + modifierLower + " -> " + parsedLevels);
    }

    /**
     * 注册特殊效果的 brewing 配方（非持续时间效果）
     * 
     * 支持链式调用，类似 event.recipes.goety.brewing()：
     * event.addSpecialBrewEffect('minecraft:bone_meal', 'grow')
     *     .soulCost(10)
     *     .capacityExtra(2);
     * 
     * 支持的特殊效果类型（通过反射自动发现，Goety 更新新效果时无需修改代码）：
     * - 'bats' - 召唤蝙蝠
     * - 'bees' - 召唤蜜蜂
     * - 'blind_jump' - 盲跳效果
     * - 'chop_tree' - 砍树效果
     * - 'combust' - 燃烧方块
     * - 'corrosion' - 腐蚀方块
     * - 'drought' - 干旱效果
     * - 'explode' - 爆炸效果
     * - 'extinguish' - 灭火效果
     * - 'fertility' - 繁殖效果
     * - 'flaying' - 剥皮效果
     * - 'flood' - 洪水效果
     * - 'freeze' - 冰冻效果
     * - 'grow' - 生长效果
     * - 'grow_cactus' - 仙人掌生长
     * - 'grow_cave_vines' - 洞穴藤蔓生长
     * - 'harvest' - 收获效果
     * - 'launch' - 发射效果
     * - 'leaf_shell' - 叶壳效果
     * - 'love' - 爱情效果
     * - 'mossify' - 苔藓化效果
     * - 'part_lava' - 部分熔岩效果
     * - 'part_water' - 部分水效果
     * - 'pulverize' - 粉碎效果
     * - 'pruning' - 修剪效果
     * - 'raise_dead' - 复活效果
     * - 'saturation' - 饱食效果
     * - 'shear' - 剪毛效果
     * - 'snow' - 雪效果
     * - 'strip' - 剥离效果
     * - 'sweet_berried' - 甜浆果效果
     * - 'thorn_trap' - 荆棘陷阱
     * - 'transpose' - 传送效果
     * - 'webbed' - 蛛网效果
     * 
     * @param item 物品ID（字符串）或物品对象
     * @param effectType 效果类型（字符串）
     * @return SpecialBrewEffectBuilder 构建器，支持链式调用
     */
    @Info(value = "注册特殊效果的 brewing 配方（支持链式调用）", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象"),
        @Param(name = "effectType", value = "效果类型（字符串），如 'bats', 'bees', 'grow', 'explode' 等")
    })
    public SpecialBrewEffectBuilder addSpecialBrewEffect(Object item, String effectType) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            ScriptType.SERVER.console.error("无法找到物品: " + item);
            return null;
        }
        
        if (effectType == null || effectType.isEmpty()) {
            ScriptType.SERVER.console.error("效果类型不能为空");
            return null;
        }
        
        return new SpecialBrewEffectBuilder(this, itemObj, effectType);
    }
    
    /**
     * 创建特殊效果的 BrewEffect 实例（供 SpecialBrewEffectBuilder 使用）
     * 包访问权限，允许 SpecialBrewEffectBuilder 调用
     */
    BrewEffect createSpecialBrewEffect(String effectType, int soulCost, int capacityExtra) {
        return createSpecialBrewEffectInternal(effectType, soulCost, capacityExtra);
    }
    
    /**
     * 创建特殊效果的 BrewEffect 实例
     * 使用反射动态发现和创建效果类，无需硬编码，Goety 更新新效果时无需修改代码
     */
    private static final java.util.Map<String, Class<? extends BrewEffect>> EFFECT_CLASS_CACHE = new java.util.HashMap<>();
    private static boolean cacheInitialized = false;
    
    /**
     * 初始化效果类缓存，扫描所有继承自 BrewEffect 的类
     * 使用多种方法确保在开发环境和打包 JAR 中都能工作
     */
    private static synchronized void initializeEffectClassCache() {
        if (cacheInitialized) {
            return;
        }
        
        try {
            // 方法1: 尝试通过已知的包路径扫描
            String[] packagesToScan = {
                "com.Polarice3.Goety.common.effects.brew",
                "com.Polarice3.Goety.common.effects.brew.block"
            };
            
            for (String packageName : packagesToScan) {
                scanPackageForBrewEffects(packageName);
            }
            
            // 方法2: 如果扫描结果为空，尝试通过 ClassLoader 查找
            if (EFFECT_CLASS_CACHE.isEmpty()) {
                tryClassLoaderScan();
            }
            
            cacheInitialized = true;
            ScriptType.SERVER.console.debug("已扫描到 " + EFFECT_CLASS_CACHE.size() + " 个 BrewEffect 子类");
        } catch (Exception e) {
            ScriptType.SERVER.console.error("初始化效果类缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 扫描指定包中的 BrewEffect 子类
     * 支持文件系统和 JAR 文件
     */
    private static void scanPackageForBrewEffects(String packageName) {
        try {
            String packagePath = packageName.replace('.', '/');
            java.net.URL resource = BrewEffect.class.getClassLoader().getResource(packagePath);
            if (resource == null) {
                return;
            }
            
            if ("file".equals(resource.getProtocol())) {
                // 文件系统（开发环境）
                java.io.File directory = new java.io.File(resource.getFile());
                if (directory.exists()) {
                    scanDirectory(directory, packageName);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                // JAR 文件（打包环境）
                scanJarPackage(packagePath);
            }
        } catch (Exception e) {
            // 忽略扫描错误，继续尝试其他方式
        }
    }
    
    /**
     * 扫描 JAR 文件中的包
     */
    private static void scanJarPackage(String packagePath) {
        try {
            java.net.URL jarUrl = BrewEffect.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl == null) {
                return;
            }
            
            try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new java.io.File(jarUrl.toURI()))) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    
                    if (name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (BrewEffect.class.isAssignableFrom(clazz) && 
                                clazz != BrewEffect.class && 
                                clazz != PotionBrewEffect.class && 
                                !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                                
                                @SuppressWarnings("unchecked")
                                Class<? extends BrewEffect> brewEffectClass = (Class<? extends BrewEffect>) clazz;
                                String effectType = classNameToEffectType(clazz.getSimpleName());
                                EFFECT_CLASS_CACHE.put(effectType.toLowerCase(), brewEffectClass);
                            }
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            // 忽略无法加载的类
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略 JAR 扫描错误
        }
    }
    
    /**
     * 递归扫描目录
     */
    private static void scanDirectory(java.io.File directory, String packageName) {
        java.io.File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (BrewEffect.class.isAssignableFrom(clazz) && 
                        clazz != BrewEffect.class && 
                        clazz != PotionBrewEffect.class && 
                        !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                        
                        @SuppressWarnings("unchecked")
                        Class<? extends BrewEffect> brewEffectClass = (Class<? extends BrewEffect>) clazz;
                        String effectType = classNameToEffectType(clazz.getSimpleName());
                        EFFECT_CLASS_CACHE.put(effectType.toLowerCase(), brewEffectClass);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // 忽略无法加载的类
                }
            }
        }
    }
    
    /**
     * 通过 ClassLoader 尝试扫描类（作为后备方案）
     * 使用反射查找所有已加载的类
     */
    private static void tryClassLoaderScan() {
        try {
            // 尝试通过反射访问 ClassLoader 的类缓存（如果可用）
            // 这是一个更通用的方法，但可能在某些环境中不可用
            java.lang.reflect.Method getPackagesMethod = Package.class.getMethod("getPackages");
            Package[] packages = (Package[]) getPackagesMethod.invoke(null);
            
            for (Package pkg : packages) {
                String packageName = pkg.getName();
                if (packageName.startsWith("com.Polarice3.Goety.common.effects.brew")) {
                    // 尝试通过包名查找类（这种方法有限，但作为后备）
                    // 实际扫描主要依赖文件系统或 JAR 扫描
                }
            }
        } catch (Exception e) {
            // 忽略错误，这是后备方案
        }
    }
    
    /**
     * 将类名转换为效果类型名称
     * 例如: BatsBrewEffect -> "bats", GrowBlockEffect -> "grow", ChopTreeBlockEffect -> "chop_tree"
     */
    private static String classNameToEffectType(String className) {
        // 移除 "BrewEffect" 或 "BlockEffect" 后缀
        String name = className;
        if (name.endsWith("BrewEffect")) {
            name = name.substring(0, name.length() - "BrewEffect".length());
        } else if (name.endsWith("BlockEffect")) {
            name = name.substring(0, name.length() - "BlockEffect".length());
        }
        
        // 将驼峰命名转换为下划线命名
        return camelToSnake(name);
    }
    
    /**
     * 将驼峰命名转换为下划线命名
     * 例如: BlindJump -> "blind_jump", GrowCactus -> "grow_cactus"
     */
    private static String camelToSnake(String camel) {
        if (camel.isEmpty()) {
            return camel;
        }
        
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camel.charAt(0)));
        
        for (int i = 1; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * 创建特殊效果的 BrewEffect 实例
     * 通过反射尝试不同的构造函数签名
     */
    private BrewEffect createSpecialBrewEffectInternal(String effectType, int soulCost, int capacityExtra) {
        try {
            initializeEffectClassCache();
            
            Class<? extends BrewEffect> effectClass = EFFECT_CLASS_CACHE.get(effectType.toLowerCase());
            if (effectClass == null) {
                // 如果直接查找失败，尝试模糊匹配
                for (java.util.Map.Entry<String, Class<? extends BrewEffect>> entry : EFFECT_CLASS_CACHE.entrySet()) {
                    if (entry.getKey().contains(effectType.toLowerCase()) || 
                        effectType.toLowerCase().contains(entry.getKey())) {
                        effectClass = entry.getValue();
                        break;
                    }
                }
            }

            if (effectClass == null) {
                ScriptType.SERVER.console.warn("未找到效果类型: " + effectType + 
                    "，可用效果: " + String.join(", ", EFFECT_CLASS_CACHE.keySet()));
                return null;
            }
            
            // 尝试不同的构造函数签名
            // 1. 无参构造函数
            try {
                java.lang.reflect.Constructor<? extends BrewEffect> constructor = effectClass.getConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException e) {
                // 继续尝试其他构造函数
            }
            
            // 2. (int soulCost) 构造函数
            try {
                java.lang.reflect.Constructor<? extends BrewEffect> constructor = effectClass.getConstructor(int.class);
                return constructor.newInstance(soulCost);
            } catch (NoSuchMethodException e) {
                // 继续尝试其他构造函数
            }
            
            // 3. (int soulCost, int capacityExtra) 构造函数
            try {
                java.lang.reflect.Constructor<? extends BrewEffect> constructor = 
                    effectClass.getConstructor(int.class, int.class);
                return constructor.newInstance(soulCost, capacityExtra);
            } catch (NoSuchMethodException e) {
                // 继续尝试其他构造函数
            }
            
            // 如果所有标准构造函数都失败，尝试查找所有公共构造函数
            java.lang.reflect.Constructor<?>[] constructors = effectClass.getConstructors();
            for (java.lang.reflect.Constructor<?> constructor : constructors) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == 0) {
                    return (BrewEffect) constructor.newInstance();
                } else if (paramTypes.length == 1 && paramTypes[0] == int.class) {
                    return (BrewEffect) constructor.newInstance(soulCost);
                } else if (paramTypes.length == 2 && 
                          paramTypes[0] == int.class && paramTypes[1] == int.class) {
                    return (BrewEffect) constructor.newInstance(soulCost, capacityExtra);
                }
            }
            
            ScriptType.SERVER.console.error("无法为效果类型 " + effectType + 
                " 找到合适的构造函数。类: " + effectClass.getName());
            return null;
            
        } catch (Exception e) {
            ScriptType.SERVER.console.error("创建特殊效果失败: " + effectType + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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
        if (level < 0) {
            ScriptType.SERVER.console.error("增强剂等级必须 >= 0，当前值: " + level);
            return;
        }

        Item itemObj = getItem(item);
        if (itemObj == null) {
            return;
        }
        
        if (modifier == null || modifier.isEmpty()) {
            ScriptType.SERVER.console.error("增强类型不能为空");
            return;
        }
        
        // 验证增强类型
        String modifierLower = modifier.toLowerCase();
        if (!isValidModifier(modifierLower)) {
            ScriptType.SERVER.console.error("无效的增强类型: " + modifier + "，有效值: capacity, duration, amplifier, aoe, linger, quaff, velocity, aquatic, fire_proof, hidden, splash, lingering, gas");
            return;
        }
        
        try {
            BrewModifier brewModifier;
            if ("capacity".equals(modifierLower)) {
                brewModifier = new CapacityModifier(level);
            } else {
                brewModifier = new BrewModifier(modifierLower, level);
            }
            if (BrewEffects.INSTANCE instanceof BrewEffectsInvoker invoker) {
                invoker.forceModifierRegister_(brewModifier, itemObj);
            } else {
                modifierRegisterMethod.invoke(BrewEffects.INSTANCE, brewModifier, itemObj);
            }
            ScriptType.SERVER.console.info("✓ 已注册增强剂: " + itemObj + " -> " + modifier + " (等级: " + level + ")");
        } catch (Exception e) {
            ScriptType.SERVER.console.error("注册增强剂失败: " + e.getMessage());
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
        int removed = com.kubejs.goety.brew.BrewData.removeCapacityItem(itemObj);
        if (removed > 0) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<Item, BrewModifier> map =
                        (java.util.Map<Item, BrewModifier>) modifiersField.get(BrewEffects.INSTANCE);
                if (map != null) {
                    map.remove(itemObj);
                }
            } catch (Exception e) {
                ScriptType.SERVER.console.error("移除容量剂映射失败: " + e.getMessage());
                e.printStackTrace();
            }
            ScriptType.SERVER.console.info("✓ 已移除容量剂: " + itemObj);
        } else {
            ScriptType.SERVER.console.warn("未找到容量剂: " + itemObj);
        }
    }
    
    /**
     * 移除物品催化剂
     * 通过反射访问 BrewEffects 的内部 Map 来删除代码中注册的配方
     */
    @Info(value = "移除物品催化剂", params = {
        @Param(name = "item", value = "物品ID（字符串）或物品对象")
    })
    public void removeCatalyst(Object item) {
        Item itemObj = getItem(item);
        if (itemObj == null) {
            ScriptType.SERVER.console.error("无法解析物品: " + item);
            return;
        }
        
        ScriptType.SERVER.console.info("尝试移除催化剂: " + itemObj);
        
        try {
            // 通过反射访问 BrewEffects.INSTANCE 的私有 catalyst Map
            java.lang.reflect.Field catalystField = com.Polarice3.Goety.common.effects.brew.BrewEffects.class.getDeclaredField("catalyst");
            catalystField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.world.item.Item, com.Polarice3.Goety.common.effects.brew.BrewEffect> catalystMap = 
                (java.util.Map<net.minecraft.world.item.Item, com.Polarice3.Goety.common.effects.brew.BrewEffect>) 
                catalystField.get(com.Polarice3.Goety.common.effects.brew.BrewEffects.INSTANCE);
            
            if (catalystMap == null) {
                ScriptType.SERVER.console.error("catalyst Map 为 null，无法移除");
                return;
            }
            
            ScriptType.SERVER.console.info("当前 catalyst Map 大小: " + catalystMap.size());
            ScriptType.SERVER.console.info("检查是否包含物品: " + itemObj + " -> " + catalystMap.containsKey(itemObj));
            
            if (catalystMap.containsKey(itemObj)) {
                com.Polarice3.Goety.common.effects.brew.BrewEffect removed = catalystMap.remove(itemObj);
                ScriptType.SERVER.console.info("✓ 已移除催化剂: " + itemObj + " (效果: " + (removed != null ? removed.getEffectID() : "null") + ")");
            } else {
                ScriptType.SERVER.console.warn("未找到催化剂: " + itemObj);
                // 列出所有已注册的催化剂，帮助调试
                ScriptType.SERVER.console.info("已注册的催化剂列表:");
                for (java.util.Map.Entry<net.minecraft.world.item.Item, com.Polarice3.Goety.common.effects.brew.BrewEffect> entry : catalystMap.entrySet()) {
                    ScriptType.SERVER.console.info("  - " + entry.getKey() + " -> " + entry.getValue().getEffectID());
                }
            }
        } catch (NoSuchFieldException e) {
            ScriptType.SERVER.console.error("找不到 catalyst 字段: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            ScriptType.SERVER.console.error("移除催化剂失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 移除实体催化剂
     * 通过反射访问 BrewEffects 的内部 Map 来删除代码中注册的配方
     */
    @Info(value = "移除实体催化剂", params = {
        @Param(name = "entity", value = "实体类型ID（字符串）")
    })
    public void removeEntityCatalyst(Object entity) {
        if (entity == null) {
            ScriptType.SERVER.console.error("实体类型不能为 null");
            return;
        }
        
        EntityType<?> entityType = null;
        if (entity instanceof EntityType) {
            entityType = (EntityType<?>) entity;
        } else if (entity instanceof CharSequence) {
            ResourceLocation location = ResourceLocation.tryParse(entity.toString());
            if (location != null) {
                entityType = ForgeRegistries.ENTITY_TYPES.getValue(location);
            }
        }
        
        if (entityType == null) {
            ScriptType.SERVER.console.error("无法解析实体类型: " + entity);
            return;
        }
        
        try {
            // 通过反射访问 BrewEffects.INSTANCE 的私有 sacrifice Map
            java.lang.reflect.Field sacrificeField = com.Polarice3.Goety.common.effects.brew.BrewEffects.class.getDeclaredField("sacrifice");
            sacrificeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<net.minecraft.world.entity.EntityType<?>, com.Polarice3.Goety.common.effects.brew.BrewEffect> sacrificeMap = 
                (java.util.Map<net.minecraft.world.entity.EntityType<?>, com.Polarice3.Goety.common.effects.brew.BrewEffect>) 
                sacrificeField.get(com.Polarice3.Goety.common.effects.brew.BrewEffects.INSTANCE);
            
            if (sacrificeMap != null && sacrificeMap.containsKey(entityType)) {
                sacrificeMap.remove(entityType);
                ScriptType.SERVER.console.info("✓ 已移除实体催化剂: " + entityType);
            } else {
                ScriptType.SERVER.console.warn("未找到实体催化剂: " + entityType);
            }
        } catch (Exception e) {
            ScriptType.SERVER.console.error("移除实体催化剂失败: " + e.getMessage());
            e.printStackTrace();
        }
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

        int removed = com.kubejs.goety.brew.BrewData.removeAugmentationItem(itemObj);
        if (removed > 0) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<Item, BrewModifier> map =
                        (java.util.Map<Item, BrewModifier>) modifiersField.get(BrewEffects.INSTANCE);
                if (map != null) {
                    map.remove(itemObj);
                }
            } catch (Exception e) {
                ScriptType.SERVER.console.error("移除增强剂映射失败: " + e.getMessage());
                e.printStackTrace();
            }
            ScriptType.SERVER.console.info("✓ 已移除增强剂: " + itemObj);
        } else {
            ScriptType.SERVER.console.warn("未找到增强剂: " + itemObj);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private Item getItem(Object item) {
        if (item == null) {
            ScriptType.SERVER.console.error("物品不能为 null");
            return null;
        }
        
        if (item instanceof Item) {
            return (Item) item;
        }
        
        if (item instanceof ItemStack) {
            return ((ItemStack) item).getItem();
        }
        
        // ItemStackJS doesn't have getItem() in KubeJS 2001
        if (item instanceof net.minecraft.world.item.ItemStack) {
            return ((net.minecraft.world.item.ItemStack) item).getItem();
        }
        
        // Try to convert to ItemStack
        try {
            var stack = dev.latvian.mods.kubejs.item.InputItem.of(item);
            if (stack != null && !stack.isEmpty()) {
                // Get the first item from the ingredient
                var items = stack.ingredient.getItems();
                if (items.length > 0) {
                    return items[0].getItem();
                }
            }
        } catch (Exception e) {
            // 忽略，继续尝试其他方法
        }
        
        String itemId = item.toString();
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location != null) {
            Item found = ForgeRegistries.ITEMS.getValue(location);
            if (found != null && found != net.minecraft.world.item.Items.AIR) {
                return found;
            }
        }
        
        ScriptType.SERVER.console.error("无法找到物品: " + item);
        return null;
    }
    
    private EntityType<?> getEntityType(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            ScriptType.SERVER.console.error("实体ID不能为空");
            return null;
        }
        
        ResourceLocation location = ResourceLocation.tryParse(entityId);
        if (location != null) {
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(location);
            if (entityType != null) {
                return entityType;
            }
        }
        
        ScriptType.SERVER.console.error("无法找到实体类型: " + entityId);
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

    private BrewData.AugmentationLevel parseAugmentationLevel(String modifier, Object entry, int index) {
        Object valueObj = entry;
        Object costObj = null;

        if (entry instanceof Map<?, ?> map) {
            valueObj = firstMapValue(map, "value", "delta");
            costObj = firstMapValue(map, "cost", "costMultiplier", "multiplier");
            if (valueObj == null) {
                throw new IllegalArgumentException("增强剂等级表第 " + index + " 项缺少 value/delta: " + entry);
            }
        }

        float value = parsePositiveFloat(valueObj, "增强剂等级表第 " + index + " 项 value");
        if (requiresWholeAugmentationValue(modifier) && Math.abs(value - Math.round(value)) > 0.0001F) {
            throw new IllegalArgumentException("增强剂 " + modifier + " 的 value 必须是整数，第 " + index + " 项为: " + value);
        }

        float cost = costObj == null
                ? BrewData.getDefaultAugmentationCost(modifier, index)
                : parsePositiveFloat(costObj, "增强剂等级表第 " + index + " 项 cost");

        return new BrewData.AugmentationLevel(value, cost);
    }

    private Object firstMapValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private float parsePositiveFloat(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }

        Object cast = UtilsJS.cast(value);
        float parsed;
        if (cast instanceof Number number) {
            parsed = number.floatValue();
        } else {
            try {
                parsed = Float.parseFloat(cast.toString());
            } catch (Exception e) {
                throw new IllegalArgumentException(name + " 必须是数字: " + value);
            }
        }

        if (parsed <= 0.0F) {
            throw new IllegalArgumentException(name + " 必须 > 0，当前值: " + parsed);
        }
        return parsed;
    }

    private boolean requiresWholeAugmentationValue(String modifier) {
        return modifier.equals("duration") ||
               modifier.equals("amplifier") ||
               modifier.equals("aoe") ||
               modifier.equals("quaff");
    }
}
