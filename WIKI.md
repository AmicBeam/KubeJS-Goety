# KubeJS Goety Wiki

Complete guide for customizing Goety mod features using KubeJS scripts.

## Table of Contents

- [Ritual System](#ritual-system)
  - [Modifying Existing Rituals](#modifying-existing-rituals)
  - [Creating New Rituals](#creating-new-rituals)
  - [Configuration Options](#configuration-options)
  - [Built-in Rituals Reference](#built-in-rituals-reference)
- [Brew System](#brew-system)
  - [Capacity Modifiers](#capacity-modifiers)
  - [Augmentation Modifiers](#augmentation-modifiers)
- [Recipe System](#recipe-system)
  - [Ritual Recipes](#ritual-recipes)
  - [Brewing Recipes](#brewing-recipes)
  - [Pulverize Recipes](#pulverize-recipes)
  - [Cursed Infuser Recipes](#cursed-infuser-recipes)
  - [Brazier Recipes](#brazier-recipes)
  - [Soul Absorber Recipes](#soul-absorber-recipes)
  - [Recipe Modification](#recipe-modification)

---

## Ritual System

The ritual system allows you to customize the structural and environmental requirements for Goety rituals.

### Modifying Existing Rituals

Use `GoetyEvents.modifyRitual` to change the conditions for existing rituals:

```javascript
GoetyEvents.modifyRitual(event => {
    // Simple block requirement
    event.modify('storm', ritual => {
        ritual.blocks = [
            '8x #minecraft:copper_ores',
            '3x minecraft:lightning_rod'
        ];
        ritual.setWeather('thunder');
        ritual.setMinY(128);
        ritual.setRequireSkyVisible(true);
    });
    
    // Custom requirement function
    event.modify('magic', ritual => {
        ritual.setRequirement((tileEntity, pos, level) => {
            // Custom logic here
            return true;
        });
    });
});
```

### Creating New Rituals

Use `GoetyEvents.registerRitual` to create custom rituals:

```javascript
GoetyEvents.registerRitual(event => {
    // Simple ritual
    event.create('diamond_ritual', ritual => {
        ritual.blocks = [
            '5x minecraft:diamond_block',
            '3x minecraft:emerald_block'
        ];
    });
    
    // Complex ritual with conditions and callbacks
    event.create('thunder_ritual', ritual => {
        ritual.blocks = ['minecraft:lightning_rod'];
        ritual.setWeather('thunder');
        ritual.setRequireSkyVisible(true);
        ritual.setJeiIcon('minecraft:lightning_rod');
        
        // Completion callback - triggered when recipe completes
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

### Configuration Options

The `ritual` object supports the following configuration methods:

#### Block Requirements

```javascript
ritual.blocks = [
    '9x minecraft:stone',           // Specific count
    '#minecraft:planks',            // Tag
    '/prismarine/',                 // Regex pattern
    '@minecraft'                    // All blocks from mod
];
```

Supported formats:
- `'minecraft:stone'` - Single block (default 1x)
- `'9x minecraft:stone'` - Specific quantity
- `'/pattern/'` or `'16x /pattern/'` - Regex matching
- `'#minecraft:planks'` - Tag
- `'@minecraft'` - All blocks from specified mod

#### Environmental Conditions

```javascript
// Dimension requirement
ritual.setDimension('minecraft:the_nether');           // Exact match
ritual.setDimension('aether', true);                   // Contains match

// Weather requirement
ritual.setWeather('thunder');  // 'thunder', 'rain', or 'clear'

// Time requirement
ritual.setTimeOfDay('night');  // 'day' or 'night'

// Biome requirement
ritual.setBiome('minecraft:plains');                   // Single biome ID
ritual.setBiome(['minecraft:plains', 'minecraft:forest'], 'id');  // Multiple IDs (OR logic)
ritual.setBiome('#minecraft:is_nether', 'tags');      // Biome tag
ritual.setBiome('coldEnoughToSnow', 'func');          // Method call on biome

// Height requirements
ritual.setMinY(128);  // Minimum Y level
ritual.setMaxY(32);   // Maximum Y level

// Sky and water requirements
ritual.setRequireSkyVisible(true);        // Requires sky visibility
ritual.setRequireAltarWaterlogged(true);  // Requires altar to be waterlogged
```

#### Display and Callbacks

```javascript
// JEI icon (optional, defaults to obsidian)
ritual.setJeiIcon('minecraft:diamond');

// Completion callback (optional)
// Note: This callback is triggered when the recipe completes, i.e., after the ritual successfully executes and produces results
// Parameters:
// - world: World object (Level) - for accessing world data and performing world operations
// - darkAltarPos: Dark altar position (BlockPos)
// - tileEntity: Dark altar block entity (DarkAltarBlockEntity)
//   * Access ritual state: tileEntity.currentRitualRecipe, tileEntity.currentTime, etc.
//   * Access consumed ingredients: tileEntity.consumedIngredients
//   * Access casting player ID: tileEntity.castingPlayerId
//   * Access converted entity: tileEntity.getConvertEntity (if any)
// - castingPlayer: Casting player (Player) - may be null if player is offline or doesn't exist
// - activationItem: Activation item (ItemStack) - the item used to start the ritual
//   * Can check item type, NBT data, etc.
//   * Note: This is a copy from ritual start, won't affect the item in player's hand
ritual.setOnFinish((world, darkAltarPos, tileEntity, castingPlayer, activationItem) => {
    // Get coordinates
    let x = darkAltarPos.getX ? darkAltarPos.getX() : darkAltarPos.x;
    let y = darkAltarPos.getY ? darkAltarPos.getY() : darkAltarPos.y;
    let z = darkAltarPos.getZ ? darkAltarPos.getZ() : darkAltarPos.z;
    
    // Use commands to play sound and particles (server-side)
    let server = world.getServer ? world.getServer() : null;
    if (server) {
        // Play sound to all players
        server.runCommandSilent(`playsound minecraft:item.firecharge.use master @a ${x} ${y} ${z} 1 1`);
        
        // Spawn particles
        server.runCommandSilent(`particle minecraft:flame ${x + 0.5} ${y + 1} ${z + 0.5} 1 1 1 0 20`);
    }
    
    // Send message to player
    if (castingPlayer) {
        castingPlayer.displayClientMessage('§6Ritual completed!', false);  // Chat message
        castingPlayer.displayClientMessage('§cFire ignited...', true);     // Action bar
    }
    
    // Access ritual data
    console.log('Consumed ingredients:', tileEntity.consumedIngredients);
    console.log('Activation item:', activationItem.getId());
});
```

**Callback Parameter Details**:

- **`tileEntity` (DarkAltarBlockEntity)**: The dark altar's block entity, providing access to:
  - `currentRitualRecipe` - The current ritual recipe being executed
  - `currentTime` - Current ritual execution time
  - `consumedIngredients` - List of ingredients consumed during the ritual
  - `castingPlayerId` - UUID of the player who started the ritual
  - `getConvertEntity` - The entity being converted (if applicable)
  - Other ritual state data

- **`activationItem` (ItemStack)**: The item used to activate the ritual:
  - This is a snapshot of the item when the ritual started
  - You can check its type, NBT data, count, etc.
  - Modifying this item won't affect the player's inventory
  - Useful for conditional logic based on the activation item's properties

#### Custom Requirement Function

```javascript
// Overrides all configuration checks
ritual.setRequirement((tileEntity, pos, level) => {
    // Fully custom logic
    return true;
});
```

### OR Logic with Multiple Condition Groups

Use multiple functions to create OR logic between condition groups:

```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('frost',
        // Condition group 1: Structure check
        ritual => {
            ritual.blocks = [
                '16x #minecraft:ice',
                '8x #minecraft:snow',
                '4x goety:freezing_lamp'
            ];
        },
        // Condition group 2: Biome check (OR logic)
        ritual => {
            ritual.setBiome('coldEnoughToSnow', 'func');
        }
    );
});
```

**Important**: Within each condition group, all conditions use AND logic. Between groups, OR logic applies.

### Disabling Built-in Rituals

```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('storm', ritual => {
        ritual.setRequirement(() => false);  // Always returns false
    });
});
```

### Built-in Rituals Reference

Goety includes 14 built-in ritual types. Below are examples showing how to modify each ritual's structure requirements using `GoetyEvents.modifyRitual`:

```javascript
// Modify all built-in rituals (complete configuration matching Goety's original implementation)
GoetyEvents.modifyRitual(event => {
    
    // 1. forge - Forge Ritual
    // Requirements: 1 lava cauldron, 2 furnaces OR blast furnaces, 1 anvil
    event.modify('forge',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                'minecraft:lava_cauldron',
                '2x minecraft:furnace',
                'minecraft:anvil'
            ];
        },
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                'minecraft:lava_cauldron',
                '2x minecraft:blast_furnace',
                'minecraft:anvil'
            ];
        }
    );
    
    // 2. animation - Animation Ritual
    // Requirements: 15 ladders, 15 rails, 1 carved pumpkin
    event.modify('animation', ritual => {
        ritual.range = 16;
        ritual.blocks = [
            '15x minecraft:ladder',
            '15x #minecraft:rails',
            'minecraft:carved_pumpkin'
        ];
    });
    
    // 3. magic - Magic Ritual
    // Requirements: 16 enchanting power (bookshelves), 1 lectern with book, 1 enchanting table
    event.modify('magic', ritual => {
        ritual.range = 16;
        ritual.setRequirement((tileEntity, pos, level) => {
            const RANGE = ritual.range || 16;
            let enchantPower = 0;
            let lecternCount = 0;
            let enchantTableCount = 0;
            
            const Blocks = Java.loadClass('net.minecraft.world.level.block.Blocks');
            const LecternBlockEntity = Java.loadClass('net.minecraft.world.level.block.entity.LecternBlockEntity');
            
            for (let i = -RANGE; i <= RANGE; i++) {
                for (let j = -RANGE; j <= RANGE; j++) {
                    for (let k = -RANGE; k <= RANGE; k++) {
                        const blockPos = pos.offset(i, j, k);
                        const blockState = level.getBlockState(blockPos);
                        const block = blockState.getBlock();
                        
                        const power = blockState.getEnchantPowerBonus(level, blockPos);
                        if (power > 0) enchantPower += power;
                        
                        if (block === Blocks.LECTERN) {
                            const blockEntity = level.getBlockEntity(blockPos);
                            if (blockEntity && LecternBlockEntity.class.isInstance(blockEntity)) {
                                const lectern = Java.cast(blockEntity, LecternBlockEntity);
                                if (!lectern.getBook().isEmpty()) lecternCount++;
                            }
                        }
                        
                        if (block === Blocks.ENCHANTING_TABLE) enchantTableCount++;
                    }
                }
            }
            
            return enchantPower >= 16 && lecternCount >= 1 && enchantTableCount >= 1;
        });
    });
    
    // 4. frost - Frost Ritual
    // Requirements: 16 ice, 8 snow, 4 freezing lamps OR cold biome
    event.modify('frost',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '16x #minecraft:ice',
                '8x #minecraft:snow',
                '4x goety:freezing_lamp'
            ];
        },
        ritual => {
            ritual.setBiome('coldEnoughToSnow', 'func');
        }
    );
    
    // 5. necroturgy - Necroturgy Ritual
    // Requirements: 16 sculk, 16 slabs, 8 flower pots with flowers, nighttime, sky light
    event.modify('necroturgy', ritual => {
        ritual.range = 16;
        ritual.setRequirement((tileEntity, pos, level) => {
            const RANGE = ritual.range || 16;
            let sculkCount = 0, slabCount = 0, flowerPotCount = 0;
            
            const Blocks = Java.loadClass('net.minecraft.world.level.block.Blocks');
            const SculkBlock = Java.loadClass('net.minecraft.world.level.block.SculkBlock');
            const SlabBlock = Java.loadClass('net.minecraft.world.level.block.SlabBlock');
            const FlowerPotBlock = Java.loadClass('net.minecraft.world.level.block.FlowerPotBlock');
            
            for (let i = -RANGE; i <= RANGE; i++) {
                for (let j = -RANGE; j <= RANGE; j++) {
                    for (let k = -RANGE; k <= RANGE; k++) {
                        const blockPos = pos.offset(i, j, k);
                        const blockState = level.getBlockState(blockPos);
                        const block = blockState.getBlock();
                        
                        if (SculkBlock.class.isInstance(block)) sculkCount++;
                        if (SlabBlock.class.isInstance(block)) slabCount++;
                        
                        if (FlowerPotBlock.class.isInstance(block)) {
                            const flowerPotBlock = Java.cast(block, FlowerPotBlock);
                            if (flowerPotBlock.getContent() !== Blocks.AIR) flowerPotCount++;
                        }
                    }
                }
            }
            
            const structureCheck = sculkCount >= 16 && slabCount >= 16 && flowerPotCount >= 8;
            const timeCheck = level.getSkyDarken() >= 4 && level.dimensionType().hasSkyLight();
            
            return structureCheck && timeCheck;
        });
    });
    
    // 6. geoturgy - Geoturgy Ritual
    // Requirements: 8 amethyst, 1 smithing table, 16 deepslate OR (no sky AND Y≤32)
    event.modify('geoturgy',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '8x minecraft:amethyst_block',
                'minecraft:smithing_table',
                '16x #minecraft:deepslate'
            ];
        },
        ritual => {
            ritual.setRequireSkyVisible(false);
            ritual.setMaxY(32);
        }
    );
    
    // 7. sky - Sky Ritual
    // Requirements: 8 marble, 16 jade, 4 indented gold OR Y≥128 OR Aether dimension
    event.modify('sky',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '8x #goety:marble_blocks',
                '16x #goety:jade_blocks',
                '4x #goety:indented_gold_blocks'
            ];
        },
        ritual => {
            ritual.setMinY(128);
        },
        ritual => {
            ritual.setDimension('aether', true);
        }
    );
    
    // 8. storm - Storm Ritual
    // Requirements: 12 copper, 4 lightning rods, 20 chains, sky conditions, thunder, sky visible
    event.modify('storm',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '12x #minecraft:copper_ores',
                '4x minecraft:lightning_rod',
                '20x minecraft:chain'
            ];
            ritual.setWeather('thunder');
            ritual.setRequireSkyVisible(true);
            ritual.setMinY(128);
        },
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '12x #minecraft:copper_ores',
                '4x minecraft:lightning_rod',
                '20x minecraft:chain'
            ];
            ritual.setWeather('thunder');
            ritual.setRequireSkyVisible(true);
            ritual.setDimension('aether', true);
        },
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '12x /copper/',
                '4x minecraft:lightning_rod',
                '20x minecraft:chain',
                '8x #goety:marble_blocks',
                '16x #goety:jade_blocks',
                '4x #goety:indented_gold_blocks'
            ];
            ritual.setWeather('thunder');
            ritual.setRequireSkyVisible(true);
        }
    );
    
    // 9. sabbath - Sabbath Ritual
    // Requirements: 8 crying obsidian, 16 obsidian, 4 soul fire
    event.modify('sabbath', ritual => {
        ritual.range = 16;
        ritual.blocks = [
            '8x minecraft:crying_obsidian',
            '16x minecraft:obsidian',
            '4x minecraft:soul_fire'
        ];
    });
    
    // 10. adept_nether - Adept Nether Ritual
    // Requirements: 8 basalt, 16 blackstone, 4 glowstone, Nether dimension OR biome
    event.modify('adept_nether',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '8x /basalt/',
                '16x /blackstone/',
                '4x minecraft:glowstone'
            ];
            ritual.setDimension('minecraft:the_nether');
        },
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '8x /basalt/',
                '16x /blackstone/',
                '4x minecraft:glowstone'
            ];
            ritual.setBiome('#minecraft:is_nether', 'tags');
        }
    );
    
    // 11. expert_nether - Expert Nether Ritual
    // Requirements: 4 wither skulls, 32 nether bricks, 8 nether wart, Nether dimension OR biome
    event.modify('expert_nether',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '4x minecraft:wither_skeleton_skull',
                '32x minecraft:nether_bricks',
                '8x minecraft:nether_wart'
            ];
            ritual.setDimension('minecraft:the_nether');
        },
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '4x minecraft:wither_skeleton_skull',
                '32x minecraft:nether_bricks',
                '8x minecraft:nether_wart'
            ];
            ritual.setBiome('#minecraft:is_nether', 'tags');
        }
    );
    
    // 12. end - End Ritual
    // Requirements: 16 void blocks, 64 end stone/bricks, 32 purpur, End dimension OR biome
    event.modify('end',
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '16x goety:void_block',
                '64x #goety:end_stone',
                '32x #minecraft:purpur'
            ];
            ritual.setDimension('minecraft:the_end');
        },
        ritual => {
            ritual.range = 16;
            ritual.blocks = [
                '16x goety:void_block',
                '64x #goety:end_stone',
                '32x #minecraft:purpur'
            ];
            ritual.setBiome('#minecraft:is_end', 'tags');
        }
    );
    
    // 13. deep - Deep Ritual
    // Requirements: 4 sea lanterns, 16 prismarine, 16 granite, deep ocean OR structure, waterlogged altar
    event.modify('deep',
        ritual => {
            ritual.setRequireAltarWaterlogged(true);
            ritual.setMaxY(63);
            ritual.setBiome('#minecraft:is_deep_ocean', 'tags');
        },
        ritual => {
            ritual.range = 16;
            ritual.setRequireAltarWaterlogged(true);
            ritual.blocks = [
                '4x minecraft:sea_lantern',
                '16x /prismarine/',
                '16x /granite/'
            ];
        }
    );
});
```

**Note**: "lich" is not a separate ritual type. Lich-related rituals use `craftType: "necroturgy"` with `research: "forbidden"` in recipe definitions.

---

## Brew System

Configure Goety's brewing system with capacity modifiers and augmentations.

### Capacity Modifiers

Capacity modifiers determine the base capacity level of brews:

```javascript
GoetyEvents.registerBrew(event => {
    // event.addCapacity(item, level)
    // - item: Item ID (string) or item object
    // - level: Level (0-7)
    
    event.addCapacity('minecraft:nether_wart', 0);
    event.addCapacity('mymod:magic_crystal', 6);
    event.addCapacity('mymod:soul_crystal', 7);
});
```

### Augmentation Modifiers

Augmentation modifiers enhance brew properties:

```javascript
GoetyEvents.registerBrew(event => {
    // event.addAugmentation(item, modifier, level)
    // - item: Item ID (string) or item object
    // - modifier: Augmentation type (string)
    // - level: Level (integer)
    
    event.addAugmentation('minecraft:redstone', 'duration', 0);
    event.addAugmentation('mymod:time_crystal', 'duration', 3);
    event.addAugmentation('mymod:power_crystal', 'amplifier', 2);
    event.addAugmentation('mymod:range_crystal', 'aoe', 1);
});
```

**Available augmentation types**:
- `'capacity'` - Capacity
- `'duration'` - Duration
- `'amplifier'` - Effect amplifier
- `'aoe'` - Area of effect
- `'linger'` - Linger effect
- `'quaff'` - Quaff effect
- `'velocity'` - Velocity
- `'aquatic'` - Aquatic
- `'fire_proof'` - Fire proof

**Complete Example**:

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

**Note**: To add catalysts (brewing recipes), use `event.recipes.goety.brewing` in the recipe system.

---

## Recipe System

Use `ServerEvents.recipes` to configure Goety's recipe system.

### Ritual Recipes

Create recipes that use the Dark Altar ritual system:

```javascript
ServerEvents.recipes(event => {
    // Basic crafting ritual
    event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', [
        'minecraft:gold_ingot',
        'minecraft:gold_ingot',
        'minecraft:diamond'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('forge')
        .soulCost(5)
        .duration(20);
    
    // Summoning ritual
    event.recipes.goety.ritual('goety:jei_dummy/none', 'goety:summon', [
        'minecraft:rotten_flesh',
        'minecraft:rotten_flesh',
        'minecraft:bone'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('necroturgy')
        .soulCost(10)
        .duration(30)
        .entityToSummon('minecraft:zombie')
        .summonLife(6000);
    
    // Sacrifice ritual (crafting with sacrifice requirement)
    event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', [
        'minecraft:gold_ingot'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('magic')
        .soulCost(20)
        .duration(40)
        .entityToSacrificeTag('minecraft:is_animal')
        .entityToSacrificeDisplayName('Animal');
    
    // Enchanting ritual
    event.recipes.goety.ritual('minecraft:enchanted_book', 'goety:enchant', [
        'goety:dark_metal_block',
        '#forge:obsidian',
        '#forge:storage_blocks/lapis'
    ])
        .activationItem('minecraft:book')
        .craftType('magic')
        .soulCost(250)
        .duration(10)
        .enchantment('minecraft:mending')
        .xpLevelCost(10);
});
```

**Ritual Type Selection**:

The second parameter (`ritualType`) determines the ritual's behavior:

- **`goety:craft`** - Crafting ritual (produces items) - Most common
- **`goety:enchant`** - Enchanting ritual (applies enchantments)
- **`goety:summon`** - Summoning ritual (spawns entities)
- **`goety:summon_tamed`** - Summoning ritual (spawns tamed entities)
- **`goety:convert`** - Conversion ritual (transforms entities)
- **`goety:convert_tamed`** - Conversion ritual (transforms tamed entities)
- **`goety:convert_complete_tamed`** - Complete conversion ritual (fully transforms tamed entities)
- **`goety:teleport`** - Teleportation ritual

**Important**: Choose the correct `ritualType` based on what the ritual does, not based on the `craftType`. For example:
- Use `goety:summon` for summoning, even if `craftType` is 'necroturgy'
- Use `goety:craft` for item crafting, regardless of `craftType`
- For summoning rituals, set `result` to `'goety:jei_dummy/none'` since the output is an entity, not an item

**Available methods**:
- `.activationItem(item)` - Item used to activate ritual
- `.craftType(type)` - Ritual structure type (forge, animation, magic, necroturgy, etc.) - This determines which ritual structure is required
- `.soulCost(cost)` - Soul cost
- `.duration(seconds)` - Duration in seconds
- `.entityToSummon(entityId)` - Entity to summon
- `.summonLife(ticks)` - Summoned entity lifespan (-1 for permanent)
- `.entityToSacrificeTag(tag)` - Required sacrifice entity tag
- `.entityToSacrificeDisplayName(name)` - Sacrifice display name
- `.entityToConvertTag(tag)` - Entity tag to convert
- `.entityToConvertDisplayName(name)` - Convert display name
- `.entityToConvertInto(entityId)` - Entity to convert into
- `.enchantment(enchantmentId)` - Enchantment to apply
- `.xpLevelCost(levels)` - XP level cost
- `.research(researchId)` - Required research (e.g., 'forbidden', 'haunting')

### Brewing Recipes

Create catalyst recipes for the brewing system:

```javascript
ServerEvents.recipes(event => {
    // Basic brewing recipe
    event.recipes.goety.brewing('minecraft:golden_apple', 'minecraft:regeneration')
        .soulCost(15)
        .capacityExtra(0)
        .duration(30);  // 30 seconds
    
    // Recipe requiring specific entity
    event.recipes.goety.brewing('minecraft:nether_star', 'minecraft:resistance')
        .soulCost(50)
        .capacityExtra(10)
        .duration(3600)
        .entityType('minecraft:ender_dragon');
    
    // Recipe requiring entity tag
    event.recipes.goety.brewing('minecraft:emerald', 'minecraft:luck')
        .soulCost(25)
        .capacityExtra(5)
        .duration(2400)
        .entityTag('minecraft:is_animal');
});
```

**Available methods**:
- `.soulCost(cost)` - Soul cost
- `.capacityExtra(extra)` - Extra capacity
- `.duration(ticks)` - Effect duration in ticks
- `.entityType(entityId)` - Required entity type
- `.entityTag(tag)` - Required entity tag

**Note**: The first parameter is the ingredient (catalyst), and the second is the effect ID (not an item).

### Pulverize Recipes

Create recipes for the Haunted block pulverizer:

```javascript
ServerEvents.recipes(event => {
    // Block result
    event.recipes.goety.pulverize('minecraft:cobblestone')
        .blockResult('minecraft:gravel');
    
    // Item result
    event.recipes.goety.pulverize('minecraft:gravel')
        .itemResult('minecraft:flint');
});
```

**Available methods**:
- `.blockResult(blockId)` - Block result (optional)
- `.itemResult(item)` - Item result (optional)

**Note**: At least one result type must be specified. If both are set, `blockResult` takes priority.

### Cursed Infuser Recipes

Create recipes for the Cursed Infuser:

```javascript
ServerEvents.recipes(event => {
    // Basic recipe
    event.recipes.goety.cursed_infuser_recipes('minecraft:emerald', 'minecraft:iron_sword')
        .cookingTime(100);  // 5 seconds
    
    // Grim recipe
    event.recipes.goety.cursed_infuser_recipes('minecraft:emerald', '#forge:obsidian')
        .grim(true)
        .cookingTime(20);
});
```

**Available methods**:
- `.cookingTime(ticks)` - Cooking time in ticks (default 60)
- `.grim(boolean)` - Whether this is a grim recipe (default false)

### Brazier Recipes

Create recipes for the Brazier:

```javascript
ServerEvents.recipes(event => {
    event.recipes.goety.brazier('minecraft:emerald', [
        'minecraft:soul_sand',
        'minecraft:soul_sand',
        'minecraft:soul_sand',
        'minecraft:lapis_lazuli'
    ])
        .soulCost(10);
});
```

**Available methods**:
- `.soulCost(cost)` - Soul cost (default 0)

### Soul Absorber Recipes

Create recipes for the Soul Absorber:

```javascript
ServerEvents.recipes(event => {
    // Basic recipe
    event.recipes.goety.soul_absorber_recipes('minecraft:soul_sand')
        .soulIncrease(25)
        .cookingTime(100);
    
    // Tag-based recipe
    event.recipes.goety.soul_absorber_recipes('#minecraft:soul_fire_base_blocks')
        .soulIncrease(25)
        .cookingTime(100);
    
    // High-value recipe
    event.recipes.goety.soul_absorber_recipes('minecraft:sculk')
        .soulIncrease(5)
        .cookingTime(50);
});
```

**Available methods**:
- `.soulIncrease(amount)` - Soul increase amount (default 25)
- `.cookingTime(ticks)` - Cooking time in ticks (default 200)

**Note**: Soul absorber recipes have no output item. They consume items to increase soul value.

### Recipe Modification

#### Method 1: Replace Input/Output (Recommended)

Use KubeJS's built-in replacement methods for batch modifications:

```javascript
ServerEvents.recipes(event => {
    // Replace ingredient in all ritual recipes
    event.replaceInput(
        { type: 'goety:ritual' },
        'minecraft:diamond',
        'minecraft:emerald'
    );
    
    // Replace activation item
    event.replaceInput(
        { type: 'goety:ritual' },
        'minecraft:ender_pearl',
        'minecraft:ender_eye'
    );
    
    // Replace output
    event.replaceOutput(
        { type: 'goety:ritual' },
        'goety:dark_robe',
        'goety:frost_robe'
    );
    
    // Replace in specific recipe
    event.replaceInput(
        { id: 'goety:grim_infuser' },
        '#goety:crypt_stone',
        'minecraft:obsidian'
    );
    
    // Global replacement (all Goety recipes)
    event.replaceInput(
        { mod: 'goety' },
        'minecraft:gold_ingot',
        'minecraft:iron_ingot'
    );
});
```

#### Method 2: Remove and Recreate

Use this method when you need to modify special fields like `soulCost`, `duration`, or `craftType`:

```javascript
ServerEvents.recipes(event => {
    // Remove existing recipe
    event.remove({ id: 'goety:void_robe' });
    
    // Recreate with modifications
    event.recipes.goety.ritual('goety:void_robe', 'goety:craft', [
        'minecraft:gold_ingot',
        'minecraft:gold_ingot',
        'minecraft:dragon_breath'
    ])
        .activationItem('minecraft:ender_pearl')
        .craftType('end')
        .soulCost(50)
        .duration(60);
});
```

**Important Warnings**:

⚠️ **DO NOT** use `craftType` as a filter condition:
```javascript
// ❌ DANGEROUS - May delete all or unpredictable recipes
event.remove({ type: 'goety:ritual', craftType: 'magic' });
```

✅ **Correct ways** to remove recipes:
```javascript
// Use exact recipe ID
event.remove({ id: 'goety:void_robe' });

// Use recipe ID pattern
event.remove({ id: /^goety:enchant\// });

// Use output filter
event.remove({ type: 'goety:ritual', output: 'goety:dark_robe' });
```

---

## Localization (Optional)

If you create custom rituals, you can add localization files for display names in JEI:

Create language files in your world save directory:
```
your_world_save/
└── kubejs/
    └── assets/
        └── goety/
            └── lang/
                ├── en_us.json
                └── zh_cn.json
```

**en_us.json**:
```json
{
  "jei.goety.craftType.diamond_ritual": "Diamond Ritual",
  "jei.goety.craftType.flame_ritual": "Flame Ritual"
}
```

**Localization key format**: `jei.goety.craftType.<ritualId>`

If no localization is provided, the ritual ID will be displayed as-is.

---

## Tips and Best Practices

1. **Start Simple**: Begin with basic block requirements before adding complex conditions
2. **Use Tags**: Prefer tags over specific blocks for flexibility
3. **Test Incrementally**: Test each ritual modification individually
4. **Use OR Logic**: Leverage multiple condition groups for alternative requirements
5. **Document Custom Rituals**: Add comments explaining your custom ritual logic
6. **Batch Modifications**: Use `replaceInput`/`replaceOutput` for bulk recipe changes
7. **Avoid craftType Filters**: Never use `craftType` in `event.remove()` filters
8. **Check Recipe IDs**: Use `/kubejs dump_recipes` to find exact recipe IDs

---

## Common Patterns

### Creating a Dimension-Specific Ritual

```javascript
event.create('nether_ritual', ritual => {
    ritual.blocks = ['8x minecraft:netherrack'];
    ritual.setDimension('minecraft:the_nether');
});
```

### Creating a Weather-Dependent Ritual

```javascript
event.create('rain_ritual', ritual => {
    ritual.blocks = ['4x minecraft:water'];
    ritual.setWeather('rain');
});
```

### Creating a Height-Restricted Ritual

```javascript
event.create('sky_high_ritual', ritual => {
    ritual.blocks = ['minecraft:beacon'];
    ritual.setMinY(200);
    ritual.setRequireSkyVisible(true);
});
```

### Creating a Biome-Specific Ritual

```javascript
event.create('desert_ritual', ritual => {
    ritual.blocks = ['8x minecraft:sand'];
    ritual.setBiome('#minecraft:is_desert', 'tags');
});
```

---

## Troubleshooting

**Ritual not working?**
- Check that all block requirements are met
- Verify environmental conditions (weather, time, dimension)
- Ensure the scan range is sufficient
- Check console for error messages

**Recipe not appearing?**
- Verify recipe syntax is correct
- Check that all required fields are set
- Reload recipes with `/reload` or `/kubejs reload server_scripts`
- Check JEI for recipe conflicts

**Brew not registering?**
- Ensure capacity level is between 0-7
- Verify augmentation modifier type is valid
- Check for duplicate registrations

---

## Additional Resources

- [KubeJS Documentation](https://kubejs.com/)
- [Goety Mod](https://github.com/Polarice3/Goety-2)
- [Example Scripts](src/main/resources/kubejs/server_scripts/)

---

*This wiki is based on KubeJS Goety example scripts. For the latest updates, check the mod's GitHub repository.*
