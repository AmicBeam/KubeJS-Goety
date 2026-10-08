# Mixin Compatibility Notes

Compile target: Goety 2.5.55.4 for Minecraft 1.20.1.
Additional static review: Goety 2.5.58.4 and Goety Revelation 2.3.4fix2
(embedded RevelationFix 4.7). Runtime coexistence remains to be verified.

## Overwrite Removal

`BrewCauldronCapacityMixin` no longer overwrites
`BrewCauldronBlockEntity.insertItem`.

The mixin now injects at the method head and only handles the behavior owned by
KubeJS Goety:

- scripted initial cauldron capacity;
- scripted capacity levels;
- scripted duration, amplifier, area, linger, quaff, and velocity levels.

Every other item path continues through Goety's original method. This includes
cauldron crafting, recipe lookup, catalysts, non-levelled modifiers, and any
future branches Goety adds before returning.

Capacity activation/upgrades are intercepted only after `setCapacityLevels`.
Augmentations are intercepted independently per type only after that type is
configured with `setAugmentationLevels`. Registering items does not claim their
level rules. An unconfigured type returns from the hook before any item insertion
or failure, allowing newer Goety/addon branches to run. An explicit empty
capacity table disables upgrades rather than falling back to six levels.

Goety 2.5.58.4 still allocates 32 slots; RevelationFix 4.7 expands those
containers to 128 and adds capacity levels 7-9. Scripted capacity tables grow
the brewing inventory to initial capacity plus the level increments, capped
at 256. Constructor-tail and pre-scan hooks cover new and already constructed
cauldrons. A load-head hook grows storage before Goety restores items, including
on clients receiving saved capacity. Growth never shrinks addon storage or
discards ingredients. Goety/addons retain ownership of the crafting inventory.
Unconfigured upgrades retain upstream limits. The 256-slot cap preserves
vanilla's unsigned-byte slot indices (0-255) without a new NBT format.

The cauldron `load(CompoundTag)` override is a Minecraft method: its production
SRG name is `m_142466_`. Its injection explicitly enables remapping even though
the mixin's Goety-specific targets use `remap = false`. Leaving this selector
unmapped caused the reported 1.1.1 startup failure on Goety 2.5.58.4.

`build` now runs `testBrewMixinMapping` after reobfuscation. It reads the packaged
Mixin annotations and refmap and resolves all six injection targets against
the unmodified production Goety jar. This catches the missing `load` mapping
without starting Minecraft. Use `-PgoetyProductionJar=/path/to/goety.jar` to
check a different production release; this is not an in-game coexistence test.
The check passed against the cached Goety 2.5.55.4 release and the cauldron
class extracted from the official Goety 2.5.58.4 release with its ZIP CRC
verified. The original crash was a target-resolution failure, not a level-table
or capacity calculation failure.

The injected branch is cancellable because a scripted level must replace
Goety's hard-coded level table after it accepts the item. It does not copy the
rest of Goety's brewing or crafting state machine.

## Mixin Audit

| Mixin | Technique | Risk | Current decision |
| --- | --- | --- | --- |
| `BrewCauldronCapacityMixin` | `@Inject` at `insertItem` head, constructor tail, scan heads, and load head | Medium | Keep selective scripted-level interception and grow-only brewing storage; monitor method names and load ordering. |
| `BrewCauldronCraftingStarterMixin` | `@ModifyArg` on the first `ItemStack.is(Item)` call in `insertItem` | Low | Keep. It only replaces Goety's hard-coded nightshade comparison argument. |
| `BrewCauldronBlockEntityMixin` | Redirect `new BrewEffects()` in three methods | Medium | Keep. It is the narrowest way to make Goety use the script-populated singleton without replacing those methods. |
| `BrewEffectInstanceMixin` | Redirect `new BrewEffects()` in `load` | Low | Keep. The target method is small and has one constructor call. |
| `BrewingCatalystProcessorMixin` | Redirect `new BrewEffects()` in Patchouli setup/render paths | Low | Keep while Goety constructs temporary registries there. |
| `BrewingSacrificeProcessorMixin` | Redirect `new BrewEffects()` in Patchouli setup/render paths | Low | Keep while Goety constructs temporary registries there. |
| `BrewEffectsMixin` | Shadow the modifier map and implement a bridge method | Low | Keep. No target method body is replaced. |

The constructor redirects remain necessary because Goety creates fresh
`BrewEffects` instances instead of exposing one registry service to consumers.
Replacing them with head injections would duplicate the target methods and
would be more fragile. The clean long-term solution is an upstream registry
API or consistent use of `BrewEffects.INSTANCE`.

## Revelation Compatibility

An early Mixin plugin disables KubeJS Goety's three cauldron mixins when it
detects RevelationFix 4.4 or older. Those versions transform the same cauldron
methods before KubeJS Goety can inject.
The fallback prevents a hard startup failure and reports the exact conflicting
mod/version through the log, a client toast, and a login chat message. It does
not disable ritual, recipe, or non-cauldron brew registration features.

Goety Awaken 1.3.8 does not need this fallback. Its cauldron mixin injects only
at the tail of `tick` to accelerate `soulTime` from an accelerated soul candle;
it does not overwrite or inject into `insertItem` or `getBrew`.

## Upgrade Checklist

For a new Goety release:

1. Compile against the new jar and run a Mixin application smoke test.
2. Confirm `insertItem(ItemStack)` still exists. Its internal branches no
   longer need to be copied or synchronized.
3. Confirm the first `ItemStack.is(Item)` call in `insertItem` is still the
   cauldron crafting starter comparison.
4. Confirm the redirected methods still contain `new BrewEffects()`. A missing
   constructor target means Goety may already use a shared registry, or the
   redirect target needs updating.
5. Confirm `container` is still a mutable `NonNullList<ItemStack>`, scan methods
   remain `getBrew`, `getOccupiedSlots`, and `getFirstEmptySlot`, and `load`
   allocates the brew inventory using `getContainerSize()` before loading items.
6. Exercise the configured cauldron starter, one cauldron recipe, one catalyst,
   one sacrifice, and at
   least one scripted capacity and augmentation level in game.


## NeoForge 1.21.1 Port

The section above describes Forge 1.20.1. The independent NeoForge tree compiles against Goety 3.1.4 and keeps Mojang production method names; it does not package an SRG refmap.

- Cauldron loading injects into `loadAdditional(CompoundTag, HolderLookup.Provider)` before restoring items, preserving expanded inventories with the 1.21 registry-aware item format.
- Goety 3.1.4 adds a `ResourceLocation` recipe ID argument to `DarkAltarBlockEntity.startRitual`; the start callback includes that argument.
- The 1.21.1 build validates six cauldron selectors, nine brew bytecode anchors and ritual/cauldron callback descriptors against the production jar.
- Dedicated-server tests cover actual Mixin application, custom callbacks, 68-slot capacity/save-load, recipe decoding and script reload. Client Patchouli anchors are checked statically; client UI and addon coexistence have not been playtested.

See [the 1.21.1 README](versions/1.21.1/README.md) for the exact dependency matrix and reproducible smoke fixtures.
