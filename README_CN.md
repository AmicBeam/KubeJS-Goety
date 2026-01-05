# KubeJS Goety

**其他语言: [English](README.md)**

KubeJS 与 Goety 模组的集成，允许通过 JavaScript 脚本自定义 Goety 的仪式构建条件、药酿系统和配方系统。

## 功能特性

- ✅ 使用 GoetyEvents 事件系统
- ✅ 支持创建和修改仪式类型，完全自定义仪式条件检查逻辑
- ✅ 支持配置药酿系统（容量剂、催化剂、增强剂）
- ✅ 支持配置配方系统（仪式配方、酿造配方、粉碎配方等）
- ✅ 服务器端脚本支持（server_scripts）
- ✅ 无需修改 Goety 模组本身

## 前置要求

- Minecraft 1.20.1
- Forge 47.1.65+
- KubeJS 2001.6+
- Goety 2.5.41+

## 安装

1. 将模组放入 `mods` 文件夹
2. 确保已安装 KubeJS 和 Goety 模组
3. 启动游戏

## 仪式系统配置

### GoetyEvents.modifyRitual 和 GoetyEvents.registerRitual

使用 GoetyEvents 事件系统来自定义 Goety 仪式的构建条件。

#### 创建脚本文件

在世界存档目录下创建脚本文件：
```
你的世界存档/
└── kubejs/
    └── server_scripts/
        └── goety_rituals.js
```

#### 修改现有仪式条件

```javascript
GoetyEvents.modifyRitual(event => {
    // event.modify(ritualId, modifier)
    // - ritualId: 要修改的仪式ID（字符串）
    // - modifier: 修改器函数，接收一个 ritual 对象
    
    // 使用配置化方式修改
    event.modify('storm', ritual => {
        ritual.blocks = ['8x #minecraft:copper_ores', '3x minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // 需要雷雨天气
        ritual.setMinY(128);                  // 高度 >= 128
        ritual.setRequireSkyVisible(true);    // 需要能看到天空
    });
    
    // 使用自定义函数修改
    event.modify('magic', ritual => {
        ritual.setRequirement((tileEntity, pos, level) => {
            // 自定义检查逻辑
            // 返回 true 表示条件满足，false 表示不满足
            return true;
        });
    });
});
```

#### 创建新仪式类型

```javascript
GoetyEvents.registerRitual(event => {
    // event.create(ritualId, builder)
    // - ritualId: 仪式的唯一标识符（字符串）
    // - builder: 构建器函数，接收一个 ritual 对象
    
    // 创建简单仪式（只需要方块）
    event.create('diamond_ritual', ritual => {
        ritual.blocks = ['5x minecraft:diamond_block', '3x minecraft:emerald_block'];
    });
    
    // 创建复杂仪式（需要特殊条件和完成效果）
    event.create('thunder_ritual', ritual => {
        ritual.blocks = ['minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // 需要雷雨天气
        ritual.setRequireSkyVisible(true);    // 需要能看到天空
        ritual.setJeiIcon('minecraft:lightning_rod');  // 设置 JEI 显示图标（可选）
        
        // 设置仪式完成时的回调（可选）
        // 注意：此回调在配方完成时触发，即仪式成功执行并产生结果后
        ritual.setOnFinish((world, darkAltarPos, tileEntity, castingPlayer, activationItem) => {
            // 获取坐标
            let x = darkAltarPos.getX ? darkAltarPos.getX() : darkAltarPos.x;
            let y = darkAltarPos.getY ? darkAltarPos.getY() : darkAltarPos.y;
            let z = darkAltarPos.getZ ? darkAltarPos.getZ() : darkAltarPos.z;
            
            // 使用命令播放音效（服务器端）
            let server = world.getServer ? world.getServer() : null;
            if (server) {
                server.runCommandSilent(`playsound minecraft:entity.lightning_bolt.thunder weather @a ${x} ${y} ${z} 1 1`);
            }
        });
    });
});
```

#### 禁用内置仪式

如果需要禁用某个内置仪式，可以使用 `modifyRitual` 让条件检查始终返回 `false`：

```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('storm', ritual => {
        ritual.requirement = () => false; // 始终返回 false，禁用仪式
    });
});
```

#### 配置选项

`ritual` 对象支持以下配置选项：

- `ritual.range` (integer): 扫描范围（默认16）
- `ritual.blocks` (array/string): 方块需求配置
  - 支持格式：`'9x minecraft:stone'`、`'/pattern/'`、`'#tag'`、`'@mod'`
  - 正则示例：`'16x /prismarine/'` 匹配所有包含 prismarine 的方块
- `ritual.setDimension(dimensionId, containsMatch)` (string, boolean): 维度限制
  - `dimensionId`: 维度ID，如 'minecraft:the_nether' 或 'aether'
  - `containsMatch`: false（精确匹配，默认）或 true（模糊匹配）
- `ritual.setWeather(weather)` (string): 天气要求（'thunder'/'rain'/'clear'）
- `ritual.setTimeOfDay(timeOfDay)` (string): 时间要求（'day'/'night'）
- `ritual.setBiome(biomeValue, type)` (object, string): 生物群系要求
  - `biomeValue`: 生物群系值（可以是字符串或数组），或方法名（当 type='func' 时）
  - `type`: 'id'（生物群系ID，默认）、'tags'（标签）、'func'（方法调用）
  - 方法调用：`ritual.setBiome('coldEnoughToSnow', 'func')` 调用 `biome.coldEnoughToSnow(pos)` 方法
  - 支持任何 Biome 类的布尔返回值方法，通过反射自动调用
- `ritual.setMinY(y)` (integer): 最小高度要求
- `ritual.setMaxY(y)` (integer): 最大高度要求
- `ritual.setRequireSkyVisible(boolean)` (boolean): 是否需要看到天空
- `ritual.setRequireAltarWaterlogged(boolean)` (boolean): 是否需要祭坛含水
- `ritual.setJeiIcon(item)` (string/object): JEI 显示图标（物品ID或物品对象，可选，默认为黑曜石）
- `ritual.setOnFinish(callback)` (function): 仪式完成时的回调函数，在配方完成时触发（即仪式成功执行并产生结果后），接收参数 (world, darkAltarPos, tileEntity, castingPlayer, activationItem)
- `ritual.setRequirement(function)` (function): 自定义检查函数（覆盖所有配置）

**参考示例**：[goety_rituals.js.example](src/main/resources/kubejs/server_scripts/goety_rituals.js.example)

#### 本地化（可选）

如果你创建了新的仪式类型，并希望在 JEI 或游戏中显示中文名称，你需要添加本地化文件：

**方法 1：使用 KubeJS 资源包**

在世界存档目录下创建资源包结构：
```
你的世界存档/
└── kubejs/
    └── assets/
        └── goety/
            └── lang/
                ├── zh_cn.json  (中文)
                └── en_us.json   (英文，可选)
```

在 `zh_cn.json` 中添加：
```json
{
  "jei.goety.craftType.diamond_ritual": "钻石仪式",
  "jei.goety.craftType.flame_ritual": "火焰仪式"
}
```

在 `en_us.json` 中添加：
```json
{
  "jei.goety.craftType.diamond_ritual": "Diamond Ritual",
  "jei.goety.craftType.flame_ritual": "Flame Ritual"
}
```

**本地化键格式：**
- 键名：`jei.goety.craftType.<ritualId>`
- 其中 `<ritualId>` 是你创建仪式时使用的 ID

**注意：**
- 如果不添加本地化，仪式名称会显示为仪式 ID（如 `diamond_ritual`）
- 本地化是可选的，不影响仪式的功能
- 可以只添加你需要的语言文件

## 药酿系统配置

### GoetyEvents.registerBrew

配置 Goety 药酿系统，包括容量剂、催化剂和增强剂。

#### 创建脚本文件

在世界存档目录下创建脚本文件：
```
你的世界存档/
└── kubejs/
    └── server_scripts/
        └── goety_brews.js
```

#### 注册容量剂

```javascript
GoetyEvents.registerBrew(event => {
    // event.addCapacity(item, level)
    // - item: 物品ID（字符串）或物品对象
    // - level: 等级（0-7）
    
    event.addCapacity('minecraft:nether_wart', 0);
    event.addCapacity('mymod:magic_crystal', 6);
});
```

#### 注册增强剂

```javascript
GoetyEvents.registerBrew(event => {
    // event.addAugmentation(item, modifier, level)
    // - item: 物品ID（字符串）或物品对象
    // - modifier: 增强类型（字符串）
    //   - 'capacity' - 容量
    //   - 'duration' - 持续时间
    //   - 'amplifier' - 效果放大
    //   - 'aoe' - 范围效果
    //   - 'linger' - 持续效果
    //   - 'quaff' - 饮用效果
    //   - 'velocity' - 速度
    //   - 'aquatic' - 水生
    //   - 'fire_proof' - 防火
    // - level: 等级（整数）
    
    event.addAugmentation('minecraft:redstone', 'duration', 0);
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
});
```

#### 注册特殊效果配方（非药水效果）

注册非药水效果的特殊效果配方（即时效果或方块效果）：

```javascript
GoetyEvents.registerBrew(event => {
    // event.addSpecialBrewEffect(item, effectType)
    //     .soulCost(cost)        // 可选，设置灵魂消耗
    //     .capacityExtra(extra)  // 可选，设置额外容量
    // 
    // - item: 物品ID（字符串）或物品对象
    // - effectType: 效果类型（字符串），如 'bats', 'bees', 'grow', 'explode' 等
    
    // 添加生长效果配方
    event.addSpecialBrewEffect('minecraft:bone_meal', 'grow')
        .soulCost(10);
    
    // 添加爆炸效果配方
    event.addSpecialBrewEffect('minecraft:tnt', 'explode')
        .soulCost(50)
        .capacityExtra(2);
    
    // 添加繁殖效果配方（不需要参数）
    event.addSpecialBrewEffect('minecraft:egg', 'fertility');
    
    // 移除已存在的特殊效果配方（适用于内置效果和自定义效果）
    event.removeCatalyst('minecraft:bone_meal');  // 移除上面注册的 'grow' 效果
});
```

**支持的效果类型**：`bats`, `bees`, `blind_jump`, `chop_tree`, `combust`, `corrosion`, `drought`, `explode`, `extinguish`, `fertility`, `flaying`, `flood`, `freeze`, `grow`, `grow_cactus`, `grow_cave_vines`, `harvest`, `launch`, `leaf_shell`, `love`, `mossify`, `part_lava`, `part_water`, `pulverize`, `pruning`, `raise_dead`, `saturation`, `shear`, `snow`, `strip`, `sweet_berried`, `thorn_trap`, `transpose`, `webbed`

**注意**：效果类型通过反射自动发现。当 Goety 添加新效果时，无需修改代码即可使用。

**移除特殊效果配方**：使用 `event.removeCatalyst(item)` 可以移除指定物品注册的特殊效果配方。这适用于内置效果和通过 `addSpecialBrewEffect` 添加的自定义效果。

⚠️ **重要提示**：移除药酿配方后，`/reload` 无法动态刷新更改。需要**重启游戏**才能使移除生效。

#### 完整示例

```javascript
GoetyEvents.registerBrew(event => {
    // 容量剂配置
    event.addCapacity('mymod:magic_crystal', 6);
    event.addCapacity('mymod:magic_dust', 5);
    
    // 增强剂配置
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
    event.addAugmentation('mymod:range_crystal', 'aoe', 1);
    
    // 移除内置催化剂，内置催化剂无法通过recipe删除
    event.removeCatalyst('minecraft:grass');
    // ⚠️ 注意：移除药酿配方后，/reload 无法动态刷新。需要重启游戏。
});
```

**参考示例**：[goety_brews.js.example](src/main/resources/kubejs/server_scripts/goety_brews.js.example)

**注意**：
- **添加容量剂和增强剂** → 使用 `GoetyEvents.registerBrew`
- **添加特殊效果配方（非药水效果）** → 在 `GoetyEvents.registerBrew` 中使用 `event.addSpecialBrewEffect()`
- **添加催化剂（酿造配方）** → 使用 `event.recipes.goety.brewing`（见下方配方系统）

## 配方系统配置

### ServerEvents.recipes

使用 KubeJS 标准配方事件来配置 Goety 的配方系统。

#### 创建脚本文件

在世界存档目录下创建脚本文件：
```
你的世界存档/
└── kubejs/
    └── server_scripts/
        └── goety_recipes.js
```

#### 仪式配方（ritual）

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.ritual(result, ritualType, ingredients)
    // - result: 产物物品（OutputItem）
    // - ritualType: 仪式类型ID，根据功能选择：
    //   - 'goety:craft' - 制作物品（最常用）
    //   - 'goety:enchant' - 附魔
    //   - 'goety:summon' - 召唤生物
    //   - 'goety:summon_tamed' - 召唤驯服生物
    //   - 'goety:convert' - 转换实体
    //   - 'goety:teleport' - 传送
    // - ingredients: 材料数组（InputItem[]）
    
    // 制作物品的仪式
    event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', [
        'minecraft:gold_ingot',
        'minecraft:gold_ingot',
        'minecraft:diamond'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('forge')  // 锻造仪式
        .soulCost(5)
        .duration(20);
    
    // 召唤生物的仪式
    event.recipes.goety.ritual('goety:jei_dummy/none', 'goety:summon', [
        'minecraft:rotten_flesh',
        'minecraft:bone'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('necroturgy')
        .soulCost(10)
        .entityToSummon('minecraft:zombie')
        .summonLife(6000);
});
```

#### 酿造配方（brewing）

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.brewing(ingredient, effect)
    // - ingredient: 材料物品（InputItem）
    // - effect: 效果ID（String，如 'minecraft:regeneration'）
    
    event.recipes.goety.brewing('minecraft:golden_apple', 'minecraft:regeneration')
        .soulCost(15)
        .capacityExtra(0)
        .duration(600);  // 30秒
    
    // 需要特定生物的酿造配方
    event.recipes.goety.brewing('minecraft:nether_star', 'minecraft:resistance')
        .soulCost(50)
        .entityType('minecraft:ender_dragon');  // 需要末影龙
});
```

**配置优先级说明**：
- 使用 `event.recipes.goety.brewing` 可以添加新的催化剂配方，也可以调整现有效果的灵魂消耗（`.soulCost()`）
- 如果同时使用 `config/goety-brews.toml` 配置文件和 KubeJS 脚本，**KubeJS 脚本的优先级更高**，会覆盖配置文件中的设置
- 推荐使用 KubeJS 脚本进行配置，因为它更灵活且易于版本控制

#### 粉碎配方（pulverize）

**注意**：粉碎配方可以同时输出物品和方块，但JEI只会显示输出物品。

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.pulverize(ingredient)
    // - ingredient: 材料物品（InputItem）
    
    // 产生方块的粉碎配方
    event.recipes.goety.pulverize('minecraft:cobblestone')
        .blockResult('minecraft:gravel');
    
    // 产生物品的粉碎配方
    event.recipes.goety.pulverize('minecraft:gravel')
        .itemResult('minecraft:flint');
});
```

#### 诅咒注入器配方（cursed_infuser_recipes）

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.cursed_infuser_recipes(result, ingredient)
    // - result: 产物物品（OutputItem）
    // - ingredient: 材料物品（InputItem）
    
    event.recipes.goety.cursed_infuser_recipes('minecraft:emerald', 'minecraft:iron_sword')
        .cookingTime(100);  // 5秒（100 tick）

    // 带 NBT 的输出
    event.recipes.goety.cursed_infuser_recipes(
        Item.of('minecraft:potion', '{Potion:"minecraft:healing"}'),
        'minecraft:glass_bottle'
    );
});
```

#### 火盆配方（brazier）

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.brazier(result, ingredients)
    // - result: 产物物品（OutputItem）
    // - ingredients: 材料数组（InputItem[]）
    
    event.recipes.goety.brazier('minecraft:emerald', [
        'minecraft:soul_sand',
        'minecraft:soul_sand',
        'minecraft:soul_sand',
        'minecraft:lapis_lazuli'
    ])
        .soulCost(10);
});
```

#### 移除配方

```javascript
ServerEvents.recipes(event => {
    // 移除现有配方
    event.remove({ type: 'goety:ritual', craftType: 'magic' });
    event.remove({ type: 'goety:brewing', effect: 'minecraft:poison' });
    event.remove({ type: 'goety:pulverize', ingredient: 'minecraft:cobblestone' });
});
```

**参考示例**：[goety_recipes.js.example](src/main/resources/kubejs/server_scripts/goety_recipes.js.example)

## 开发

### 构建

```bash
./gradlew build
```

### 项目结构

```
kubejs-goety/
├── src/main/
│   ├── java/com/kubejs/goety/
│   │   ├── KubeJSGoety.java          # 模组主类
│   │   ├── util/
│   │   │   └── EventHandlers.java    # 事件处理器
│   │   ├── event/
│   │   │   ├── RegisterRitualEventJS.java    # 注册仪式事件
│   │   │   └── ModifyRitualEventJS.java      # 修改仪式事件
│   │   └── plugin/
│   │       └── GoetyKubeJSPlugin.java # KubeJS 插件
│   └── resources/
│       ├── META-INF/
│       │   └── mods.toml               # 模组元数据
│       ├── kubejs.plugins.txt         # 插件注册文件
│       └── kubejs/
│           └── server_scripts/
│               ├── goety_rituals.js.example  # 仪式配置示例脚本
│               ├── goety_recipes.js.example   # 配方配置示例脚本
│               └── goety_brews.js.example    # 药酿配置示例脚本
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 相关链接

- [KubeJS](https://github.com/KubeJS-Mods/KubeJS)
- [Goety](https://github.com/Polarice3/Goety-2)
