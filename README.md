# KubeJS Goety

**Read this in other languages: [简体中文](README_CN.md)**

KubeJS integration for Goety mod. Allows customizing Goety ritual requirements, brew system, and recipe system via JavaScript scripts.

## Features

- ✅ Uses GoetyEvents event system
- ✅ Create and modify ritual types with fully customizable ritual condition checking logic
- ✅ Brew system configuration (capacity modifiers, catalysts, augmentations)
- ✅ Recipe system configuration (ritual recipes, brewing recipes, pulverize recipes, etc.)
- ✅ Server-side script support (server_scripts)
- ✅ No need to modify the Goety mod itself

## Requirements

- Minecraft 1.20.1
- Forge 47.1.65+
- KubeJS 2001.6+
- Goety 2.5+

## Installation

1. Put the mod into the `mods` folder
2. Make sure KubeJS and Goety mods are installed
3. Start the game

## Ritual System Configuration

### GoetyEvents.modifyRitual and GoetyEvents.registerRitual

Use GoetyEvents event system to customize Goety ritual requirements.

#### Create Script File

Create a script file in your world save directory:
```
your_world_save/
└── kubejs/
    └── server_scripts/
        └── goety_rituals.js
```

#### Modify Existing Ritual Conditions

```javascript
GoetyEvents.modifyRitual(event => {
    // event.modify(ritualId, modifier)
    // - ritualId: Ritual ID to modify (string)
    // - modifier: Modifier function that receives a ritual object
    
    // Modify using configuration style
    event.modify('storm', ritual => {
        ritual.blocks = ['8x #minecraft:copper_ores', '3x minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // Requires thunder weather
        ritual.setMinY(128);                  // Height >= 128
        ritual.setRequireSkyVisible(true);    // Requires sky visibility
    });
    
    // Modify using custom function
    event.modify('magic', ritual => {
        ritual.setRequirement((tileEntity, pos, level) => {
            // Custom check logic
            // Return true if conditions are met, false otherwise
            return true;
        });
    });
});
```

#### Create New Ritual Type

```javascript
GoetyEvents.registerRitual(event => {
    // event.create(ritualId, builder)
    // - ritualId: Unique identifier for the ritual (string)
    // - builder: Builder function that receives a ritual object
    
    // Create simple ritual (only needs blocks)
    event.create('diamond_ritual', ritual => {
        ritual.blocks = ['5x minecraft:diamond_block', '3x minecraft:emerald_block'];
    });
    
    // Create complex ritual (needs special conditions)
    event.create('thunder_ritual', ritual => {
        ritual.blocks = ['minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // Requires thunder weather
        ritual.setRequireSkyVisible(true);    // Requires sky visibility
    });
});
```

#### Disabling Built-in Rituals

To disable a built-in ritual, use `modifyRitual` to make the condition check always return `false`:

```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('storm', ritual => {
        ritual.requirement = () => false; // Always return false to disable ritual
    });
});
```

#### Configuration Options

The `ritual` object supports the following configuration options:

- `ritual.range` (integer): Scan range (default 16)
- `ritual.blocks` (array/string): Block requirement configuration
  - Supported formats: `'9x minecraft:stone'`, `'/pattern/'`, `'#tag'`, `'@mod'`
  - Regex example: `'16x /prismarine/'` matches all blocks containing prismarine
- `ritual.setDimension(dimensionId, containsMatch)` (string, boolean): Dimension restriction
  - `dimensionId`: Dimension ID, e.g., 'minecraft:the_nether' or 'aether'
  - `containsMatch`: false (exact match, default) or true (fuzzy match)
- `ritual.setWeather(weather)` (string): Weather requirement ('thunder'/'rain'/'clear')
- `ritual.setTimeOfDay(timeOfDay)` (string): Time requirement ('day'/'night')
- `ritual.setBiome(biomeValue, type)` (object, string): Biome requirement
  - `biomeValue`: Biome value (can be string or array), or method name (when type='func')
  - `type`: 'id' (biome ID, default), 'tags' (tags), 'func' (method call)
  - Method call: `ritual.setBiome('coldEnoughToSnow', 'func')` calls `biome.coldEnoughToSnow(pos)` method
  - Supports any boolean method in Biome class, invoked via reflection
- `ritual.setMinY(y)` (integer): Minimum height requirement
- `ritual.setMaxY(y)` (integer): Maximum height requirement
- `ritual.setRequireSkyVisible(boolean)` (boolean): Whether sky visibility is required
- `ritual.setRequireAltarWaterlogged(boolean)` (boolean): Whether altar must be waterlogged
- `ritual.setRequirement(function)` (function): Custom check function (overrides all configurations)

**See example**: `src/main/resources/kubejs/server_scripts/goety_rituals.js.example`

## Why Use server_scripts?

Ritual condition checking happens on the server side, so `server_scripts` must be used instead of `startup_scripts`:

- **server_scripts**: Runs when the server loads, can access server-side game state
- **startup_scripts**: Runs during mod initialization, before the game world is loaded

## Supported Ritual Types

### Built-in Ritual Types (can be modified via modifyRitual)

- `storm` - Storm ritual
- `magic` - Magic ritual
- `necroturgy` - Necroturgy ritual
- `forge` - Forge ritual
- `geoturgy` - Geoturgy ritual
- `sabbath` - Sabbath ritual
- `adept_nether` - Adept Nether ritual
- `expert_nether` - Expert Nether ritual
- `end` - End ritual
- `frost` - Frost ritual
- `sky` - Sky ritual
- `deep` - Deep ritual
- `animation` - Animation ritual

### Custom Ritual Types

Any ritual type created via `GoetyEvents.registerRitual` can be modified.

## Brew System Configuration

### GoetyEvents.registerBrew

Configure Goety's brew system, including capacity modifiers, catalysts, and augmentations.

#### Create Script File

Create a script file in your world save directory:
```
your_world_save/
└── kubejs/
    └── server_scripts/
        └── goety_brews.js
```

#### Register Capacity Modifiers

```javascript
GoetyEvents.registerBrew(event => {
    // event.addCapacity(item, level)
    // - item: Item ID (string) or item object
    // - level: Level (0-7)
    
    event.addCapacity('minecraft:nether_wart', 0);
    event.addCapacity('mymod:magic_crystal', 6);
});
```

#### Register Augmentations

```javascript
GoetyEvents.registerBrew(event => {
    // event.addAugmentation(item, modifier, level)
    // - item: Item ID (string) or item object
    // - modifier: Augmentation type (string)
    //   - 'capacity' - Capacity
    //   - 'duration' - Duration
    //   - 'amplifier' - Effect amplifier
    //   - 'aoe' - Area of effect
    //   - 'linger' - Linger effect
    //   - 'quaff' - Quaff effect
    //   - 'velocity' - Velocity
    //   - 'aquatic' - Aquatic
    //   - 'fire_proof' - Fire proof
    // - level: Level (integer)
    
    event.addAugmentation('minecraft:redstone', 'duration', 0);
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
});
```

#### Complete Example

```javascript
GoetyEvents.registerBrew(event => {
    // Capacity modifiers
    event.addCapacity('mymod:magic_crystal', 6);
    event.addCapacity('mymod:magic_dust', 5);
    
    // Augmentations
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
    event.addAugmentation('mymod:range_crystal', 'aoe', 1);
});
```

**See example**: `src/main/resources/kubejs/server_scripts/goety_brews.js.example`

**Note**:
- **Adjust existing effect soul costs** → Use `config/goety-brews.toml` configuration file
- **Add catalysts (brewing recipes)** → Use `event.recipes.goety.brewing` (see Recipe System below)
- **Add capacity modifiers and augmentations** → Use `GoetyEvents.registerBrew`

## Recipe System Configuration

### ServerEvents.recipes

Use KubeJS standard recipe events to configure Goety's recipe system.

#### Create Script File

Create a script file in your world save directory:
```
your_world_save/
└── kubejs/
    └── server_scripts/
        └── goety_recipes.js
```

#### Ritual Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.ritual(result, ritualType, ingredients)
    // - result: Output item (OutputItem)
    // - ritualType: Ritual type ID (usually 'goety:craft')
    // - ingredients: Material array (InputItem[])
    
    event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', [
        'minecraft:gold_ingot',
        'minecraft:gold_ingot',
        'minecraft:diamond'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('forge')  // Forge ritual
        .soulCost(5)
        .duration(20);
});
```

#### Brewing Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.brewing(ingredient, effect)
    // - ingredient: Material item (InputItem)
    // - effect: Effect ID (String, e.g., 'minecraft:regeneration')
    
    event.recipes.goety.brewing('minecraft:golden_apple', 'minecraft:regeneration')
        .soulCost(15)
        .capacityExtra(0)
        .duration(1800);  // 30 seconds
    
    // Brewing recipe requiring specific entity
    event.recipes.goety.brewing('minecraft:nether_star', 'minecraft:resistance')
        .soulCost(50)
        .entityType('minecraft:ender_dragon');  // Requires ender dragon
});
```

#### Pulverize Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.pulverize(ingredient)
    // - ingredient: Material item (InputItem)
    
    // Pulverize recipe producing block
    event.recipes.goety.pulverize('minecraft:cobblestone')
        .blockResult('minecraft:gravel');
    
    // Pulverize recipe producing item
    event.recipes.goety.pulverize('minecraft:gravel')
        .itemResult('minecraft:flint');
});
```

#### Cursed Infuser Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.cursed_infuser_recipes(result, ingredient)
    // - result: Output item ID (String)
    // - ingredient: Material item (InputItem)
    
    event.recipes.goety.cursed_infuser_recipes('minecraft:emerald', 'minecraft:iron_sword')
        .cookingTime(100);  // 5 seconds (100 ticks)
});
```

#### Brazier Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.brazier(result, ingredients)
    // - result: Output item (OutputItem)
    // - ingredients: Material array (InputItem[])
    
    event.recipes.goety.brazier('minecraft:emerald', [
        'minecraft:soul_sand',
        'minecraft:soul_sand',
        'minecraft:soul_sand',
        'minecraft:lapis_lazuli'
    ])
        .soulCost(10);
});
```

#### Remove Recipes

```javascript
ServerEvents.recipes(event => {
    // Remove existing recipes
    event.remove({ type: 'goety:ritual', craftType: 'magic' });
    event.remove({ type: 'goety:brewing', effect: 'minecraft:poison' });
    event.remove({ type: 'goety:pulverize', ingredient: 'minecraft:cobblestone' });
});
```

**See example**: `src/main/resources/kubejs/server_scripts/goety_recipes.js.example`

## Development

### Build

```bash
./gradlew build
```

### Project Structure

```
kubejs-goety/
├── src/main/
│   ├── java/com/kubejs/goety/
│   │   ├── KubeJSGoety.java          # Main mod class
│   │   ├── util/
│   │   │   └── EventHandlers.java    # Event handlers
│   │   ├── event/
│   │   │   ├── RegisterRitualEventJS.java    # Register ritual event
│   │   │   └── ModifyRitualEventJS.java      # Modify ritual event
│   │   └── plugin/
│   │       └── GoetyKubeJSPlugin.java # KubeJS plugin
│   └── resources/
│       ├── META-INF/
│       │   └── mods.toml               # Mod metadata
│       ├── kubejs.plugins.txt         # Plugin registration file
│       └── kubejs/
│           └── server_scripts/
│               ├── goety_rituals.js.example  # Ritual configuration example script
│               ├── goety_recipes.js.example   # Recipe configuration example script
│               └── goety_brews.js.example    # Brew configuration example script
```

## License

MIT License

## Contributing

Issues and Pull Requests are welcome!

## Links

- [KubeJS](https://github.com/KubeJS-Mods/KubeJS)
- [Goety](https://github.com/Polarice3/Goety-2)
