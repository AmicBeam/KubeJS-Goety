# Mixin Compatibility Notes

Target reviewed: Goety 2.5.55.4 for Minecraft 1.20.1.

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

The injected branch is cancellable because a scripted level must replace
Goety's hard-coded level table after it accepts the item. It does not copy the
rest of Goety's brewing or crafting state machine.

## Mixin Audit

| Mixin | Technique | Risk | Current decision |
| --- | --- | --- | --- |
| `BrewCauldronCapacityMixin` | `@Inject` at `insertItem` head and `@ModifyConstant` in the constructor | Medium | Keep the small scripted-level interception; monitor the two `32` constructor constants. |
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

KubeJS Goety no longer skips cauldron mixins when Revelation is detected. The
old early-loading configuration plugin and `kubejs_goety.properties` retreat
switch have been removed. Compatibility now assumes a Revelation build that
uses injections instead of overwriting `insertItem` and `getBrew`.

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
5. Confirm the cauldron constructor still uses `32` for both the brew inventory
   and craft inventory.
6. Exercise the configured cauldron starter, one cauldron recipe, one catalyst,
   one sacrifice, and at
   least one scripted capacity and augmentation level in game.
