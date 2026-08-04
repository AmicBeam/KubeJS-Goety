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
- Goety 2.5.55.0+

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
    
    // Create complex ritual (needs special conditions and completion effects)
    event.create('thunder_ritual', ritual => {
        ritual.blocks = ['minecraft:lightning_rod'];
        ritual.setWeather('thunder');         // Requires thunder weather
        ritual.setRequireSkyVisible(true);    // Requires sky visibility
        ritual.setJeiIcon('minecraft:lightning_rod');  // Set JEI display icon (optional)
        
        // Set callback when ritual completes (optional)
        // Note: This callback is triggered when the recipe completes, i.e., after the ritual successfully executes and produces results
        ritual.setOnFinish((world, darkAltarPos, tileEntity, castingPlayer, activationItem) => {
            // Get coordinates
            let x = darkAltarPos.getX ? darkAltarPos.getX() : darkAltarPos.x;
            let y = darkAltarPos.getY ? darkAltarPos.getY() : darkAltarPos.y;
            let z = darkAltarPos.getZ ? darkAltarPos.getZ() : darkAltarPos.z;
            
            // Use commands to play sound (server-side)
            let server = world.getServer ? world.getServer() : null;
            if (server) {
                server.runCommandSilent(`playsound minecraft:entity.lightning_bolt.thunder weather @a ${x} ${y} ${z} 1 1`);
            }
        });
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
- `ritual.setJeiIcon(item)` (string/object): JEI display icon (item ID or item object, optional, defaults to obsidian)
- `ritual.setOnFinish(callback)` (function): Callback function triggered when the recipe completes (i.e., after the ritual successfully executes and produces results), receives (world, darkAltarPos, tileEntity, castingPlayer, activationItem)
- `ritual.setRequirement(function)` (function): Custom check function (overrides all configurations)

**See example**: [goety_rituals.js.example](src/main/resources/kubejs/server_scripts/goety_rituals.js.example)

#### Localization (Optional)

If you create new ritual types and want to display English names in JEI or in-game, you need to add localization files:

**Using KubeJS Resource Pack**

Create a resource pack structure in your world save directory:
```
your_world_save/
└── kubejs/
    └── assets/
        └── goety/
            └── lang/
                └── en_us.json
```

Add to `en_us.json`:
```json
{
  "jei.goety.craftType.diamond_ritual": "Diamond Ritual",
  "jei.goety.craftType.flame_ritual": "Flame Ritual"
}
```

**Localization Key Format:**
- Key name: `jei.goety.craftType.<ritualId>`
- Where `<ritualId>` is the ID you used when creating the ritual

**Note:**
- If you don't add localization, the ritual name will display as the ritual ID (e.g., `diamond_ritual`)
- Localization is optional and does not affect ritual functionality
- You can add only the language files you need

## Brew System Configuration

### GoetyEvents.registerBrew

Configure Goety's brew system, including capacity modifiers, augmentations, and special brew effects.

#### Create Script File

Create a script file in your world save directory:
```
your_world_save/
└── kubejs/
    └── server_scripts/
        └── goety_brews.js
```

#### Set the Cauldron Crafting Starter

```javascript
GoetyEvents.registerBrew(event => {
    // The default starter is goety:nightshade_blossom
    event.setCauldronStarter('mymod:custom_nightshade');
});
```

`setCauldronStarter` replaces the original starter. After it is set, the
nightshade blossom no longer switches an idle cauldron to `CRAFTING` mode.

#### Set Capacity Level Deltas

Capacity upgrades are driven by a level delta table. Levels start at 1 and each entry defines how much capacity is added at that level.

```javascript
GoetyEvents.registerBrew(event => {
    // event.setCapacityLevels([level1, level2, ...])
    // Example: [2,2,2,2,4] means level 1~5 add 2/2/2/2/4
    event.setCapacityLevels([2, 2, 2, 2, 4, 6, 8]);
});
```

**Limit**: The cauldron's maximum total capacity is capped at 32 (values above are clamped).

Without `setCapacityLevels`, the Goety 2.5.55 six-level delta table
`[2, 2, 2, 2, 4, 6]` is used.

#### Register Capacity Modifiers

```javascript
GoetyEvents.registerBrew(event => {
    // event.addCapacity(item, level)
    // - item: Item ID (string) or item object
    // - level: Level (>=0)
    
    event.addCapacity('minecraft:nether_wart', 0);
    event.addCapacity('mymod:magic_crystal', 6);
});
```

**Notes**:
- Level 0 activates the initial capacity
- Levels 1~N must be defined in `setCapacityLevels`
- Multiple items can share the same level; the delta comes from the level table

#### Set Augmentation Level Tables

Levelable augmentations are driven by level tables. Levels start at 0 and each entry defines the value added by that level.

```javascript
GoetyEvents.registerBrew(event => {
    // event.setAugmentationLevels(modifier, levels)
    // - modifier: Levelable augmentation type (string)
    // - levels: Array indexed from level 0
    // - Number entry: value only; soul cost uses Goety's built-in multiplier
    //   for known levels, then repeats the last built-in multiplier.
    event.setAugmentationLevels('duration', [1, 1, 1, 1, 1]);

    // Object entry: configure both value and soul cost multiplier.
    // Aliases: value/delta, cost/costMultiplier/multiplier.
    event.setAugmentationLevels('amplifier', [
        { value: 1, cost: 2.0 },
        { value: 1, cost: 2.5 },
        { value: 1, cost: 3.0 },
        { value: 1, cost: 3.5 }
    ]);
});
```

**Supported levelable types**: `duration`, `amplifier`, `aoe`, `linger`, `quaff`, `velocity`.

**Notes**:
- `addAugmentation(item, modifier, level)` uses the matching entry in the modifier's level table
- `duration`, `amplifier`, `aoe`, and `quaff` values must be whole numbers
- `linger` and `velocity` may use decimal values
- Existing Goety level tables are used unless overridden with `setAugmentationLevels`

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

**Notes**:
- Levelable modifier levels must exist in that modifier's `setAugmentationLevels` table
- Multiple items can share the same modifier level; the value and cost come from the level table

#### Remove Capacity Modifiers and Augmentations

```javascript
GoetyEvents.registerBrew(event => {
    // Remove capacity modifier
    event.removeCapacity('minecraft:nether_wart');

    // Remove augmentation
    event.removeAugmentation('minecraft:redstone');
});
```

Both removal methods support Goety's built-in registrations and script-added
registrations. They validate the modifier type currently mapped to the item.

#### Register Special Brew Effects (Non-Potion Effects)

Register special brew effects that are not potion effects (instant effects or block effects):

```javascript
GoetyEvents.registerBrew(event => {
    // event.addSpecialBrewEffect(item, effectType)
    //     .soulCost(cost)        // Optional, set soul cost
    //     .capacityExtra(extra)  // Optional, set extra capacity
    // 
    // - item: Item ID (string) or item object
    // - effectType: Effect type (string), e.g., 'bats', 'bees', 'grow', 'explode', etc.
    
    // Add growth effect recipe
    event.addSpecialBrewEffect('minecraft:bone_meal', 'grow')
        .soulCost(10);
    
    // Add explosion effect recipe
    event.addSpecialBrewEffect('minecraft:tnt', 'explode')
        .soulCost(50)
        .capacityExtra(2);
    
    // Add fertility effect recipe (no parameters needed)
    event.addSpecialBrewEffect('minecraft:egg', 'fertility');
    
    // Remove existing special brew effect (works for both built-in and custom effects)
    event.removeCatalyst('minecraft:bone_meal');  // Removes the 'grow' effect registered above
});
```

**Available effect types**: `bats`, `bees`, `blind_jump`, `chop_tree`, `combust`, `corrosion`, `drought`, `explode`, `extinguish`, `fertility`, `flaying`, `flood`, `freeze`, `grow`, `grow_cactus`, `grow_cave_vines`, `harvest`, `launch`, `leaf_shell`, `love`, `mossify`, `part_lava`, `part_water`, `pulverize`, `pruning`, `raise_dead`, `saturation`, `shear`, `snow`, `strip`, `sweet_berried`, `thorn_trap`, `transpose`, `webbed`

**Note**: Effect types are automatically discovered via reflection. When Goety adds new effects, they will be available without requiring code changes.

**Removing special brew effects**: Use `event.removeCatalyst(item)` to remove special brew effects registered for a specific item. This works for both built-in effects and custom effects added via `addSpecialBrewEffect`.

⚠️ **Important**: Brew configuration changes (capacity/augmentation/special effects/removals) do not apply via `/reload`. You must **re-enter the world or restart the server**.

#### Complete Example

```javascript
GoetyEvents.registerBrew(event => {
    // Capacity modifiers
    event.addCapacity('mymod:magic_crystal', 6);
    event.addCapacity('mymod:magic_dust', 5);
    
    // Augmentations
    event.setAugmentationLevels('duration', [1, 1, 1, 1]);
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
    event.addAugmentation('mymod:range_crystal', 'aoe', 1);

    // Remove the built-in catalyst; the built-in catalyst cannot be removed through the recipe.
    event.removeCatalyst('minecraft:grass');
    // ⚠️ Note: Brew config changes require re-entering the world or restarting the server.
});
```

**See example**: [goety_brews.js.example](src/main/resources/kubejs/server_scripts/goety_brews.js.example)

**Note**:
- **Add capacity modifiers and augmentations** → Use `GoetyEvents.registerBrew`
- **Add special brew effects (non-potion effects)** → Use `event.addSpecialBrewEffect()` in `GoetyEvents.registerBrew`
- **Add catalysts (brewing recipes)** → Use `event.recipes.goety.brewing` (see Recipe System below)

**Compatibility (Revelation)**:
- When **Revelation (`revelationfix` or `goety_revelation`)** is present, KubeJS Goety skips its cauldron brewing mixins by default. This restores the conservative fallback and avoids fighting Revelation's own cauldron overwrites.
- In that default mode, `setCapacityLevels` and custom capacity/augmentation cauldron logic do not affect Revelation's cauldron path.
- The default config file is created automatically at `config/kubejs_goety.properties`. To opt in anyway for a pack that has already handled Revelation-side compatibility, edit it before launch:
```properties
skipCauldronBrewingMixinsWithRevelation=false
```
- This file is read during mixin loading, so a full restart is required. The same value can also be set with JVM property `-Dkubejs_goety.skipCauldronBrewingMixinsWithRevelation=false`.

## Recipe System Configuration

### ServerEvents.recipes

Use KubeJS standard recipe events to configure Goety's recipe system.

Goety recipe inputs now follow KubeJS default NBT matching behavior: plain inputs ignore NBT, `.weakNBT()` performs partial matching, and `.strongNBT()` performs exact matching.

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
    // - ritualType: Ritual type ID, choose based on function:
    //   - 'goety:craft' - Craft items (most common)
    //   - 'goety:enchant' - Enchanting
    //   - 'goety:summon' - Summon entities
    //   - 'goety:summon_tamed' - Summon tamed entities
    //   - 'goety:convert' - Convert entities
    //   - 'goety:teleport' - Teleportation
    // - ingredients: Material array (InputItem[])
    
    // Crafting ritual
    event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', [
        'minecraft:gold_ingot',
        'minecraft:gold_ingot',
        'minecraft:diamond'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('forge')  // Forge ritual
        .soulCost(5)
        .duration(20);
    
    // Summoning ritual
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

#### Brewing Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.brewing(ingredient, effect)
    // - ingredient: Material item (InputItem)
    // - effect: Effect ID (String, e.g., 'minecraft:regeneration')
    
    event.recipes.goety.brewing('minecraft:golden_apple', 'minecraft:regeneration')
        .soulCost(15)
        .capacityExtra(0)
        .duration(600);  // 30 seconds
    
    // Brewing recipe requiring specific entity
    event.recipes.goety.brewing('minecraft:nether_star', 'minecraft:resistance')
        .soulCost(50)
        .entityType('minecraft:ender_dragon');  // Requires ender dragon
});
```

#### Cauldron Crafting Recipes

```javascript
ServerEvents.recipes(event => {
    // event.recipes.goety.cauldron(result, ingredients)
    event.recipes.goety.cauldron('minecraft:nether_star', [
        '2x minecraft:diamond',
        'minecraft:blaze_powder',
        '#forge:ingots/gold'
    ])
        .takeWith('minecraft:glass_bottle') // Omit to collect by hand
        .levelLeft(1)                       // Remaining water level, clamped to 1-3
        .soulCost(100)
        .color(0xB1FF7F);
});
```

Ingredients are matched without ordering. Counted inputs such as
`2x minecraft:diamond` are expanded into repeated ingredients. Configure the
item that enters crafting mode with `setCauldronStarter()` in
`GoetyEvents.registerBrew`.

**Configuration Priority**:
- You can use `event.recipes.goety.brewing` to add new catalyst recipes or adjust soul costs for existing effects (`.soulCost()`)
- If both `config/goety-brews.toml` and KubeJS scripts are used, **KubeJS scripts have higher priority** and will override config file settings
- Using KubeJS scripts is recommended as they are more flexible and easier to version control

#### Pulverize Recipes

**Note**: Pulverize recipes can output both items and blocks simultaneously, but JEI will only display the item output.

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
    // - result: Output item (OutputItem)
    // - ingredient: Material item (InputItem)
    
    event.recipes.goety.cursed_infuser_recipes('minecraft:emerald', 'minecraft:iron_sword')
        .cookingTime(100);  // 5 seconds (100 ticks)

    // Output with NBT
    event.recipes.goety.cursed_infuser_recipes(
        Item.of('minecraft:potion', '{Potion:"minecraft:healing"}'),
        'minecraft:glass_bottle'
    );
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

**See example**: [goety_recipes.js.example](src/main/resources/kubejs/server_scripts/goety_recipes.js.example)

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

## Acknowledgements and Third-Party Attribution

Limited early implementation details—the name/signature of the
`forceModifierRegister_` bridge and the initial organization of the two
capacity/augmentation item index tables—were developed with reference to the
corresponding implementation in **RevelationFix**. This attribution does not
cover KubeJS Goety's brew system as a whole; its scripting APIs, dynamic level
configuration, cauldron logic, recipes, modifier removal/replacement, and later
extensions are KubeJS Goety work. RevelationFix credits MegaDarkness as
programmer and declares **ARR (All Rights Reserved)**. See
[NOTICE.md](NOTICE.md) for the exact scope and licensing clarification.

## License

MIT License

## Contributing

Issues and Pull Requests are welcome!

## Links

- [KubeJS](https://github.com/KubeJS-Mods/KubeJS)
- [Goety](https://github.com/Polarice3/Goety-2)
