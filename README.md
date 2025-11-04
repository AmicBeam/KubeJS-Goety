# KubeJS Goety

KubeJS integration for Goety mod. Allows customizing Goety ritual requirements via JavaScript scripts.

## Features

- ✅ Uses GoetyEvents event system (similar to KubeJS-TFC design)
- ✅ Create new ritual types
- ✅ Modify existing ritual conditions
- ✅ Remove custom ritual types
- ✅ Fully customizable ritual condition checking logic
- ✅ Supports all Goety ritual types
- ✅ Server-side script support (server_scripts)
- ✅ No need to modify the Goety mod itself

## Requirements

- Minecraft 1.20.1
- NeoForge 47.1.65+
- KubeJS 2001.7.2+
- Goety 2.5.38.2+

## Installation

1. Put the mod into the `mods` folder
2. Make sure KubeJS and Goety mods are installed
3. Start the game

## Usage

### 1. Create Script File

Create a script file in your world save directory:
```
your_world_save/
└── kubejs/
    └── server_scripts/
        └── goety_rituals.js
```

### 2. Use GoetyEvents API

Scripts use the `GoetyEvents` event system, providing three main events:

#### Modify Existing Ritual Conditions

```javascript
GoetyEvents.modifyRitual(event => {
    // Modify storm ritual
    event.modify('storm', ritual => {
        ritual.name = 'storm';
        ritual.requirement = (tileEntity, pos, level) => {
            // Custom check logic
            // Return true if conditions are met, false otherwise
            return true;
        };
    });
});
```

#### Create New Ritual Type

```javascript
GoetyEvents.registerRitual(event => {
    event.create('my_custom_ritual', ritual => {
        ritual.name = 'my_custom_ritual';
        ritual.requirement = (tileEntity, pos, level) => {
            // Custom check logic
            return true;
        };
    });
});
```

#### Remove Ritual Type

```javascript
GoetyEvents.removeRitual(event => {
    // Can only remove custom-created rituals, not built-in ones
    event.remove('my_custom_ritual');
});
```

### 3. Full Example

See `src/main/resources/kubejs/server_scripts/goety_rituals.js.example` for a complete example.

## API Reference

### GoetyEvents.modifyRitual

Modify conditions for existing ritual types.

**Parameters:**
- `event.modify(ritualId, modifier)` - Modify ritual with specified ID
  - `ritualId` (string): Ritual ID to modify
  - `modifier` (function): Modifier function that receives a `ritual` object
    - `ritual.name` (string): Ritual name
    - `ritual.requirement` (function): Condition check function `(tileEntity, pos, level) => boolean`

**Example:**
```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('storm', ritual => {
        ritual.requirement = (tileEntity, pos, level) => {
            // Custom check logic
            return true;
        };
    });
});
```

### GoetyEvents.registerRitual

Create new ritual types.

**Parameters:**
- `event.create(ritualId, builder)` - Create new ritual
  - `ritualId` (string): Unique identifier for the ritual
  - `builder` (function): Builder function that receives a `ritual` object
    - `ritual.name` (string): Ritual name (optional, defaults to ritualId)
    - `ritual.requirement` (function): Condition check function `(tileEntity, pos, level) => boolean`

**Example:**
```javascript
GoetyEvents.registerRitual(event => {
    event.create('diamond_ritual', ritual => {
        ritual.name = 'diamond_ritual';
        ritual.requirement = (tileEntity, pos, level) => {
            // Check if there are enough diamond blocks
            return true;
        };
    });
});
```

### GoetyEvents.removeRitual

Remove ritual types (can only remove custom-created rituals).

**Parameters:**
- `event.remove(ritualId)` - Remove ritual with specified ID
  - `ritualId` (string): Ritual ID to remove
- `event.removeAll(...ritualIds)` - Remove multiple rituals
  - `ritualIds` (string[]): Array of ritual IDs to remove

**Note:** Cannot remove built-in rituals (such as 'storm', 'magic', etc.). To disable a built-in ritual, use `modifyRitual` and make `requirement` always return `false`.

**Example:**
```javascript
GoetyEvents.removeRitual(event => {
    event.remove('my_custom_ritual');
    // Or remove multiple
    event.removeAll('ritual1', 'ritual2', 'ritual3');
});
```

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

Any ritual type created via `GoetyEvents.registerRitual` can be modified or removed.

## Disabling Built-in Rituals

To disable a built-in ritual, use `modifyRitual` to make the condition check always return `false`:

```javascript
GoetyEvents.modifyRitual(event => {
    event.modify('storm', ritual => {
        ritual.requirement = () => false; // Always return false to disable ritual
    });
});
```

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
│   │   │   ├── ModifyRitualEventJS.java      # Modify ritual event
│   │   │   └── RemoveRitualEventJS.java      # Remove ritual event
│   │   └── plugin/
│   │       └── GoetyKubeJSPlugin.java # KubeJS plugin
│   └── resources/
│       ├── META-INF/
│       │   └── mods.toml               # Mod metadata
│       ├── kubejs.plugins.txt         # Plugin registration file
│       └── kubejs/
│           └── server_scripts/
│               └── goety_rituals.js.example # Example script
```

## License

MIT License

## Contributing

Issues and Pull Requests are welcome!

## Links

- [KubeJS](https://github.com/KubeJS-Mods/KubeJS)
- [Goety](https://github.com/Polarice3/Goety-2)
