# KubeJS Goety

KubeJS 与 Goety 模组的集成，允许通过 JavaScript 脚本自定义 Goety 仪式的构建条件。

## 功能特性

- ✅ 使用 GoetyEvents 事件系统（类似 KubeJS-TFC 的设计）
- ✅ 支持创建新仪式类型
- ✅ 支持修改现有仪式条件
- ✅ 支持删除自定义仪式类型
- ✅ 完全自定义的仪式条件检查逻辑
- ✅ 支持所有 Goety 仪式类型
- ✅ 服务器端脚本支持（server_scripts）
- ✅ 无需修改 Goety 模组本身

## 前置要求

- Minecraft 1.20.1
- NeoForge 47.1.65+
- KubeJS 2001.7.2+
- Goety 2.5.38.2+

## 安装

1. 将模组放入 `mods` 文件夹
2. 确保已安装 KubeJS 和 Goety 模组
3. 启动游戏

## 使用方法

### 1. 创建脚本文件

在世界存档目录下创建脚本文件：
```
你的世界存档/
└── kubejs/
    └── server_scripts/
        └── goety_rituals.js
```

### 2. 使用 GoetyEvents API

脚本使用 `GoetyEvents` 事件系统，提供三个主要事件：

#### 修改现有仪式条件

```javascript
GoetyEvents.modifyRitual(event => {
    // 修改风暴仪式
    event.modify('storm', ritual => {
        ritual.name = 'storm';
        ritual.requirement = (tileEntity, pos, level) => {
            // 自定义检查逻辑
            // 返回 true 表示条件满足，false 表示不满足
            return true;
        };
    });
});
```

#### 创建新仪式类型

```javascript
GoetyEvents.registerRitual(event => {
    event.create('my_custom_ritual', ritual => {
        ritual.name = 'my_custom_ritual';
        ritual.requirement = (tileEntity, pos, level) => {
            // 自定义检查逻辑
            return true;
        };
    });
});
```

#### 删除仪式类型

```javascript
GoetyEvents.removeRitual(event => {
    // 只能删除自定义创建的仪式，不能删除内置仪式
    event.remove('my_custom_ritual');
});
```

### 3. 完整示例

参考 `src/main/resources/kubejs/server_scripts/goety_rituals.js.example` 文件获取完整示例。

## API 参考

### GoetyEvents.modifyRitual

修改现有仪式类型的条件。

**参数：**
- `event.modify(ritualId, modifier)` - 修改指定 ID 的仪式
  - `ritualId` (string): 要修改的仪式 ID
  - `modifier` (function): 修改器函数，接收一个 `ritual` 对象
    - `ritual.range` (integer): 扫描范围（默认16）
    - `ritual.blocks` (object/array/string): 方块需求配置
    - `ritual.setDimension(dimensionId, containsMatch)` (string, boolean): 维度限制
      - `dimensionId`: 维度ID，如 'minecraft:the_nether' 或 'aether'
      - `containsMatch`: false（精确匹配，默认）或 true（模糊匹配）
      - 不写第二个参数时默认精确匹配：`ritual.setDimension(dimensionId)` 等同于 `setDimension(dimensionId, false)`
    - `ritual.setWeather(weather)` (string): 天气要求（'thunder'/'rain'/'clear'）
    - `ritual.setTimeOfDay(timeOfDay)` (string): 时间要求（'day'/'night'）
    - `ritual.setBiome(biomeValue, type)` (object, string): 生物群系要求
      - `biomeValue`: 生物群系值（可以是字符串或数组）
      - `type`: 'id'（生物群系ID，默认）、'tags'（标签）、'coldEnoughToSnow'（寒冷检查）
      - 不写第二个参数时默认为'id'：`ritual.setBiome(biomeValue)` 等同于 `setBiome(biomeValue, 'id')`
    - `ritual.setMinY(y)` (integer): 最小高度要求
    - `ritual.setMaxY(y)` (integer): 最大高度要求
    - `ritual.setRequireSkyVisible(boolean)` (boolean): 是否需要看到天空
    - `ritual.setRequirement(function)` (function): 自定义检查函数（覆盖所有配置）

**示例：**
```javascript
GoetyEvents.modifyRitual(event => {
    // 修改风暴仪式（使用配置化方式）
    event.modify('storm', ritual => {
        ritual.blocks = ['8x #minecraft:copper_ores', '3x minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // 需要雷雨天气
        ritual.setMinY(128);                  // 高度 >= 128
        ritual.setRequireSkyVisible(true);    // 需要能看到天空
    });
    
    // 修改霜冻仪式（使用 coldEnoughToSnow 检查）
    event.modify('frost', ritual => {
        ritual.blocks = ['minecraft:ice', 'minecraft:packed_ice'];
        ritual.setBiome(null, 'coldEnoughToSnow');  // 检查生物群系是否寒冷到可以下雪
    });
    
    // 修改下界仪式（使用生物群系标签）
    event.modify('adept_nether', ritual => {
        ritual.setBiome('#minecraft:is_nether', 'tags');  // 使用标签检查
    });
    
    // 修改 Aether 维度仪式（使用模糊匹配）
    event.modify('sky', ritual => {
        ritual.setDimension('aether', true);  // 维度ID包含 'aether'（模糊匹配）
    });
});
```

### GoetyEvents.registerRitual

创建新的仪式类型。

**参数：**
- `event.create(ritualId, builder)` - 创建新仪式
  - `ritualId` (string): 仪式的唯一标识符
  - `builder` (function): 构建器函数，接收一个 `ritual` 对象
    - `ritual.name` (string): 仪式名称（可选，默认为 ritualId）
    - `ritual.range` (integer): 扫描范围（默认16）
    - `ritual.blocks` (object/array/string): 方块需求配置
    - `ritual.setDimension(dimensionId, containsMatch)` (string, boolean): 维度限制
      - `dimensionId`: 维度ID，如 'minecraft:the_nether' 或 'aether'
      - `containsMatch`: false（精确匹配，默认）或 true（模糊匹配）
      - 不写第二个参数时默认精确匹配：`ritual.setDimension(dimensionId)` 等同于 `setDimension(dimensionId, false)`
    - `ritual.setWeather(weather)` (string): 天气要求（'thunder'/'rain'/'clear'）
    - `ritual.setTimeOfDay(timeOfDay)` (string): 时间要求（'day'/'night'）
    - `ritual.setBiome(biomeValue, type)` (object, string): 生物群系要求
      - `biomeValue`: 生物群系值（可以是字符串或数组）
      - `type`: 'id'（生物群系ID，默认）、'tags'（标签）、'coldEnoughToSnow'（寒冷检查）
      - 不写第二个参数时默认为'id'：`ritual.setBiome(biomeValue)` 等同于 `setBiome(biomeValue, 'id')`
    - `ritual.setMinY(y)` (integer): 最小高度要求
    - `ritual.setMaxY(y)` (integer): 最大高度要求
    - `ritual.setRequireSkyVisible(boolean)` (boolean): 是否需要看到天空
    - `ritual.setRequirement(function)` (function): 自定义检查函数（覆盖所有配置）

**示例：**
```javascript
GoetyEvents.registerRitual(event => {
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

### GoetyEvents.removeRitual

删除仪式类型（只能删除自定义创建的仪式）。

**参数：**
- `event.remove(ritualId)` - 删除指定 ID 的仪式
  - `ritualId` (string): 要删除的仪式 ID
- `event.removeAll(...ritualIds)` - 删除多个仪式
  - `ritualIds` (string[]): 要删除的仪式 ID 数组

**注意：** 无法删除内置仪式（如 'storm', 'magic' 等）。如果需要禁用内置仪式，请使用 `modifyRitual` 让 `requirement` 始终返回 `false`。

**示例：**
```javascript
GoetyEvents.removeRitual(event => {
    event.remove('my_custom_ritual');
    // 或删除多个
    event.removeAll('ritual1', 'ritual2', 'ritual3');
});
```

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
  - 配置示例：`ritual.setBiome(null, 'coldEnoughToSnow')` 或使用自定义函数

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

通过 `GoetyEvents.registerRitual` 创建的任何仪式类型都可以被修改或删除。

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

## 禁用内置仪式

如果需要禁用某个内置仪式，可以使用 `modifyRitual` 让条件检查始终返回 `false`：

```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('storm', ritual => {
        ritual.requirement = () => false; // 始终返回 false，禁用仪式
    });
});
```

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
│   │   │   ├── ModifyRitualEventJS.java      # 修改仪式事件
│   │   │   └── RemoveRitualEventJS.java     # 删除仪式事件
│   │   └── plugin/
│   │       └── GoetyKubeJSPlugin.java # KubeJS 插件
│   └── resources/
│       ├── META-INF/
│       │   └── mods.toml               # 模组元数据
│       ├── kubejs.plugins.txt         # 插件注册文件
│       └── kubejs/
│           └── server_scripts/
│               └── goety_rituals.js.example # 示例脚本
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 相关链接

- [KubeJS](https://github.com/KubeJS-Mods/KubeJS)
- [Goety](https://github.com/Polarice3/Goety-2)
