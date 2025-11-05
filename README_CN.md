# KubeJS Goety

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
- Goety 2.5+

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
    
    // 创建复杂仪式（需要特殊条件）
    event.create('thunder_ritual', ritual => {
        ritual.blocks = ['minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // 需要雷雨天气
        ritual.setRequireSkyVisible(true);    // 需要能看到天空
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
- `ritual.setRequirement(function)` (function): 自定义检查函数（覆盖所有配置）

**参考示例**：`src/main/resources/kubejs/server_scripts/goety_rituals.js.example`

## 为什么使用 server_scripts？

仪式条件的检查是在服务器端进行的，因此需要使用 `server_scripts` 而不是 `startup_scripts`：

- **server_scripts**: 在服务器加载时运行，可以访问服务器端的游戏状态
- **startup_scripts**: 在模组初始化时运行，此时游戏世界还未加载

## 支持的仪式类型

### 内置仪式类型（可通过 modifyRitual 修改）

#### 可以直接使用 `ritual.blocks` 配置的仪式

以下仪式只需要检查方块结构，可以直接使用 `ritual.blocks` 配置：

- `animation` - 活力仪式
- `sabbath` - 安息仪式

**注意：** `forge` (锻造仪式) 需要使用多函数参数（条件组），因为熔炉和高炉是OR逻辑。

#### 需要使用配置化方式（结合特殊条件配置）的仪式

以下仪式除了方块结构外，还需要检查其他条件，但可以通过配置化方式实现：

- `storm` - 风暴仪式
  - 配置示例：`ritual.setWeather('thunder')`, `ritual.setMinY(128)`, `ritual.setRequireSkyVisible(true)`
- `sky` - 天空仪式
  - 配置示例：`ritual.setMinY(128)`
- `adept_nether` - 进阶下界仪式
  - 配置示例：`ritual.setDimension('minecraft:the_nether')` 或 `ritual.setBiome('#minecraft:is_nether', 'tags')`
- `expert_nether` - 专家下界仪式
  - 配置示例：`ritual.setDimension('minecraft:the_nether')` 或 `ritual.setBiome('#minecraft:is_nether', 'tags')`
- `frost` - 霜冻仪式
  - 配置示例：`ritual.setBiome('coldEnoughToSnow', 'func')` 或使用自定义函数

#### 需要使用多函数参数（OR 逻辑）的仪式

以下仪式需要多个条件组，任意一个满足即可：

- `forge` - 锻造仪式（熔炉或高炉，OR逻辑）
- `frost` - 霜冻仪式（结构检查 OR 寒冷生物群系检查）
- `geoturgy` - 大地仪式（结构检查 OR 环境检查）
- `sky` - 天空仪式（结构检查 OR 高度检查）
- `adept_nether` - 进阶下界仪式（结构检查 OR 维度检查）
- `expert_nether` - 专家下界仪式（结构检查 OR 维度检查）
- `storm` - 风暴仪式（多个条件组）

#### 需要自定义函数 `ritual.setRequirement()` 的仪式

以下仪式需要特殊检查（如附魔力、讲台的书、花盆内容等），必须使用自定义函数：

- `magic` - 魔法仪式（需要检查附魔力（书架）和讲台的书）
- `necroturgy` - 死灵仪式（需要检查花盆内容：花盆必须有花）

**注意：**
- 现在大部分仪式都可以通过配置化方式实现，只有极少数需要自定义函数
- 配置化方式的检查顺序：维度 → 天气 → 时间 → 生物群系 → 高度 → 天空可见性 → 方块需求
- `lich` (大师死灵仪式) 不是一个独立的仪式类型，不支持修改
  - lich 相关的仪式使用 `craftType: "necroturgy"` 和 `research: "forbidden"`
  - 如果需要修改 lich 仪式的结构，应修改 `necroturgy` 仪式类型（但会影响所有 necroturgy 仪式）

### 自定义仪式类型

通过 `GoetyEvents.registerRitual` 创建的任何仪式类型都可以被修改。

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

#### 注册物品催化剂

```javascript
GoetyEvents.registerBrew(event => {
    // event.addCatalyst(item, effect, soulCost, duration, capacityExtra)
    // - item: 物品ID（字符串）或物品对象
    // - effect: 效果ID（字符串，如 'minecraft:strength'）
    // - soulCost: 灵魂消耗（整数）
    // - duration: 持续时间 tick（整数，可选，默认600）
    // - capacityExtra: 额外容量（整数，可选，默认0）
    
    // 完整版本
    event.addCatalyst('minecraft:glowstone_dust', 'minecraft:night_vision', 25, 1200, 0);
    
    // 简化版本（只使用必填参数）
    event.addCatalyst('mymod:essence', 'minecraft:strength', 50);
    
    // 带持续时间的版本
    event.addCatalyst('mymod:power_crystal', 'minecraft:regeneration', 60, 3600);
});
```

#### 注册实体催化剂

```javascript
GoetyEvents.registerBrew(event => {
    // event.addEntityCatalyst(entity, effect, soulCost, duration, capacityExtra)
    // - entity: 实体类型ID（字符串）或实体标签（字符串，以 # 开头）
    // - effect: 效果ID（字符串）
    // - soulCost: 灵魂消耗（整数）
    // - duration: 持续时间 tick（整数，可选，默认600）
    // - capacityExtra: 额外容量（整数，可选，默认0）
    
    // 单个实体类型
    event.addEntityCatalyst('minecraft:zombie', 'minecraft:poison', 75, 1800, 1);
    
    // 实体标签（影响所有匹配的实体）
    event.addEntityCatalyst('#minecraft:is_animal', 'minecraft:regeneration', 100);
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

#### 完整示例

```javascript
GoetyEvents.registerBrew(event => {
    // 容量剂配置
    event.addCapacity('mymod:magic_crystal', 6);
    
    // 物品催化剂配置
    event.addCatalyst('mymod:dark_essence', 'minecraft:strength', 50, 1800, 2);
    event.addCatalyst('mymod:light_crystal', 'minecraft:night_vision', 30, 2400, 1);
    
    // 实体催化剂配置
    event.addEntityCatalyst('minecraft:creeper', 'minecraft:haste', 100, 1800, 1);
    event.addEntityCatalyst('#minecraft:is_animal', 'minecraft:regeneration', 100);
    
    // 增强剂配置
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
});
```

**参考示例**：`src/main/resources/kubejs/server_scripts/goety_brews.js.example`

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
    // - ritualType: 仪式类型ID（通常是 'goety:craft'）
    // - ingredients: 材料数组（InputItem[]）
    
    event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', [
        'minecraft:gold_ingot',
        'minecraft:gold_ingot',
        'minecraft:diamond'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('forge')  // 锻造仪式
        .soulCost(5)
        .duration(20);
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
        .duration(1800);  // 30秒
    
    // 需要特定生物的酿造配方
    event.recipes.goety.brewing('minecraft:nether_star', 'minecraft:resistance')
        .soulCost(50)
        .entityType('minecraft:ender_dragon');  // 需要末影龙
});
```

#### 粉碎配方（pulverize）

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
    // - result: 产物物品ID（String）
    // - ingredient: 材料物品（InputItem）
    
    event.recipes.goety.cursed_infuser_recipes('minecraft:emerald', 'minecraft:iron_sword')
        .cookingTime(100);  // 5秒（100 tick）
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

**参考示例**：`src/main/resources/kubejs/server_scripts/goety_recipes.js.example`

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
