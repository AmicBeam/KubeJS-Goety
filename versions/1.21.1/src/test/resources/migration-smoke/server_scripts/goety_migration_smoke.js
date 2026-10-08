let smokeState = { starts: 0, finishes: 0, requirements: 0 }
GoetyEvents.registerResearch(event => {
    event.create('migration_smoke', research => {
        research.setScroll('kubejs:migration_smoke_scroll')
        research.setDisplayName('Migration Smoke')
        research.setConsumeScroll(true)
    })
    console.info('KJS_GOETY_SMOKE_RESEARCH')
})

GoetyEvents.registerRitual(event => {
    event.create('migration_smoke', ritual => {
        ritual.blocks = ['minecraft:stone']
        ritual.setOnStart((world, pos, tile, player, item) => { smokeState.starts++ })
        ritual.setOnFinish((world, pos, tile, player, item) => { smokeState.finishes++ })
        ritual.setRequirement((tile, pos, world) => { smokeState.requirements++; return true })
    })
    event.create('migration_callback_smoke', ritual => {
        ritual.setRequirement((tile, pos, world) => { smokeState.requirements++; return true })
        ritual.setOnStart((world, pos, tile, player, item) => { smokeState.starts++ })
        ritual.setOnFinish((world, pos, tile, player, item) => { smokeState.finishes++ })
    })
    console.info('KJS_GOETY_SMOKE_REGISTER_RITUAL')
})

GoetyEvents.modifyRitual(event => {
    event.modify('migration_smoke', ritual => ritual.setMinY(-64))
    console.info('KJS_GOETY_SMOKE_MODIFY_RITUAL')
})

GoetyEvents.removeRitual(event => {
    console.info('KJS_GOETY_SMOKE_REMOVE_RITUAL')
})

GoetyEvents.registerBrew(event => {
    event.setCauldronStarter('minecraft:apple')
    event.setCapacityLevels([32, 32])
    event.addCapacity('minecraft:stone', 1)
    event.addCapacity('minecraft:cobblestone', 2)
    event.setAugmentationLevels('duration', [1, 1, 2, 2])
    event.addCapacity('minecraft:nether_wart', 0)
    event.addAugmentation('minecraft:redstone', 'duration', 3)
    event.addSpecialBrewEffect('minecraft:bone_meal', 'grow').soulCost(10)
    console.info('KJS_GOETY_SMOKE_BREW')
})

ServerEvents.recipes(event => {
    let created = []
    created.push(event.recipes.goety.ritual('minecraft:diamond', 'goety:craft', ['minecraft:amethyst_shard'])
        .activationItem('minecraft:book').craftType('magic').research('migration_smoke')
        .soulCost(100).duration(30).id('kubejs:migration_smoke_ritual'))
    created.push(event.recipes.goety.brewing('minecraft:sugar', 'minecraft:speed')
        .soulCost(10).duration(600).id('kubejs:migration_smoke_brewing'))
    created.push(event.recipes.goety.cauldron('minecraft:emerald', ['2x minecraft:diamond', 'minecraft:apple'])
        .takeWith('minecraft:glass_bottle').soulCost(10).id('kubejs:migration_smoke_cauldron'))
    created.push(event.recipes.goety.pulverize('minecraft:cobblestone')
        .itemResult('minecraft:gravel').id('kubejs:migration_smoke_pulverize'))
    created.push(event.recipes.goety.cursed_infuser_recipes('minecraft:diamond', 'minecraft:coal')
        .cookingTime(60).grim(false).id('kubejs:migration_smoke_infuser'))
    created.push(event.recipes.goety.brazier('minecraft:emerald', ['minecraft:diamond'])
        .soulCost(5).id('kubejs:migration_smoke_brazier'))
    created.push(event.recipes.goety.soul_absorber_recipes('minecraft:coal')
        .soulIncrease(25).cookingTime(123).id('kubejs:migration_smoke_absorber'))
    created.push(event.recipes.goety.ritual('minecraft:emerald', 'goety:craft', ['minecraft:dirt']).id('kubejs:migration_smoke_default_ritual'))
    created.push(event.recipes.goety.cauldron('minecraft:emerald', ['minecraft:dirt']).id('kubejs:migration_smoke_default_cauldron'))
    created.push(event.recipes.goety.brewing('minecraft:apple', 'minecraft:luck').id('kubejs:migration_smoke_default_brewing'))
    created.forEach(recipe => {
        recipe.serialize()
        try { if (recipe.getOriginalRecipe() == null) throw new Error('Codec returned null') } catch (error) { throw new Error('Recipe codec failed for ' + recipe.getId() + ': ' + error) }
    })
    console.info('KJS_GOETY_SMOKE_RECIPES_CREATED')
})

ServerEvents.loaded(event => {
    let ids = []
    let iterator = event.server.getRecipeManager().getRecipes().iterator()
    while (iterator.hasNext()) {
        let holder = iterator.next()
        let id = String(holder.id())
        if (id.startsWith('kubejs:migration_smoke_')) ids.push(id)
        if (id === 'kubejs:migration_smoke_absorber' && holder.value().getCookingTime() !== 123) {
            throw new Error('Soul absorber cookingTime was not preserved')
        }
        if (id === 'kubejs:migration_smoke_infuser' && holder.value().getCookingTime() !== 60) throw new Error('Infuser default cooking time changed')
        if (id === 'kubejs:migration_smoke_cauldron' && holder.value().getIngredients().size() !== 3) {
            throw new Error('Cauldron counted ingredients were not expanded')
        }
    }
    if (ids.length !== 10) throw new Error('Expected 10 decoded Goety recipes, found ' + ids)
    console.info('KJS_GOETY_SMOKE_PASS: ' + ids.sort().join(', '))
    event.server.scheduleInTicks(30, () => event.server.runCommandSilent('reload'))
    event.server.scheduleInTicks(90, () => {
        let count = 0
        let reloaded = event.server.getRecipeManager().getRecipes().iterator()
        while (reloaded.hasNext()) if (String(reloaded.next().id()).startsWith('kubejs:migration_smoke_')) count++
        if (count !== 10) throw new Error('Recipes were not preserved after reload: ' + count)
        const Rituals = Java.loadClass('com.Polarice3.Goety.api.ritual.RitualType')
        const Pos = Java.loadClass('net.minecraft.core.BlockPos')
        let level = event.server.overworld()
        let pos = new Pos(0, 200, 0)
        if (!Rituals.getRitualType('migration_callback_smoke').getRequirement(null, pos, level)) throw new Error('Reloaded custom requirement did not execute')
        console.info('KJS_GOETY_RELOAD_PASS')
    })

    event.server.scheduleInTicks(10, () => {
        const Pos = Java.loadClass('net.minecraft.core.BlockPos')
        const Registries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')
        const Id = Java.loadClass('net.minecraft.resources.ResourceLocation')
        const Altar = Java.loadClass('com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity')
        const Cauldron = Java.loadClass('com.Polarice3.Goety.common.blocks.entities.BrewCauldronBlockEntity')
        const Rituals = Java.loadClass('com.Polarice3.Goety.api.ritual.RitualType')
        let level = event.server.overworld()
        let pos = new Pos(0, 200, 0)
        let altar = new Altar(pos, Registries.BLOCK.get(Id.parse('goety:dark_altar')).defaultBlockState())
        altar.setLevel(level)
        let ritual = Rituals.getRitualType('migration_callback_smoke')
        if (!ritual.getRequirement(altar, pos, level)) throw new Error('Custom ritual requirement failed')
        ritual.onStartRitual(level, pos, altar, null, Item.of('minecraft:apple'))
        ritual.onFinishRitual(level, pos, altar, null, Item.of('minecraft:apple'))
        if (smokeState.starts !== 1 || smokeState.finishes !== 1 || smokeState.requirements < 1) throw new Error('Ritual callbacks did not execute: ' + JSON.stringify(smokeState))
        console.info('KJS_GOETY_CALLBACKS_PASS')
        let state = Registries.BLOCK.get(Id.parse('goety:witch_cauldron')).defaultBlockState()
        level.setBlockAndUpdate(pos, Registries.BLOCK.get(Id.parse('minecraft:air')).defaultBlockState())
        level.setBlockAndUpdate(pos, state)
        event.server.runCommandSilent('setblock 0 200 0 goety:witch_cauldron[level=4]')
        state = level.getBlockState(pos)
        let cauldron = level.getBlockEntity(pos)
        if (cauldron.container.size() < 68) throw new Error('Cauldron storage did not grow')
        cauldron.mode = cauldron.insertItem(Item.of('minecraft:nether_wart'))
        cauldron.mode = cauldron.insertItem(Item.of('minecraft:stone'))
        cauldron.mode = cauldron.insertItem(Item.of('minecraft:cobblestone'))
        if (cauldron.getCapacity() !== 68) throw new Error('Scripted cauldron levels did not apply: ' + cauldron.getCapacity())
        cauldron.setItem(67, Item.of('minecraft:apple'))
        let saved = cauldron.saveWithoutMetadata(level.registryAccess())
        let restored = new Cauldron(pos, state)
        restored.setLevel(level)
        restored.loadWithComponents(saved, level.registryAccess())
        if (restored.getCapacity() !== 68 || restored.getItem(67).isEmpty()) throw new Error('Expanded cauldron inventory did not survive save/load')
        console.info('KJS_GOETY_CAULDRON_PASS')
    })
})
