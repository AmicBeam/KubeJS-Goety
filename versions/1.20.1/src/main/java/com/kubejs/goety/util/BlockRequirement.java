package com.kubejs.goety.util;

import dev.latvian.mods.kubejs.item.InputItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A parsed ritual requirement that matches blocks rather than item representations. */
public final class BlockRequirement {
    private static final Pattern COUNT_PREFIX = Pattern.compile("^\\s*(\\d+)x\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);

    private final Predicate<BlockState> predicate;
    private final int count;
    private final String description;

    private BlockRequirement(Predicate<BlockState> predicate, int count, String description) {
        this.predicate = predicate;
        this.count = count;
        this.description = description;
    }

    public static BlockRequirement parse(Object value) {
        if (value instanceof CharSequence sequence) {
            return parseString(sequence.toString());
        }

        // Keep compatibility with ItemStack/InputItem values supplied by existing scripts.
        InputItem input = InputItem.of(value);
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("empty or unsupported block requirement: " + value);
        }
        Ingredient ingredient = input.ingredient;
        return new BlockRequirement(state -> {
            var item = state.getBlock().asItem();
            return item != Items.AIR && ingredient.test(item.getDefaultInstance());
        }, input.count, String.valueOf(value));
    }

    private static BlockRequirement parseString(String source) {
        String selector = source.trim();
        int count = 1;
        Matcher countMatcher = COUNT_PREFIX.matcher(selector);
        if (countMatcher.matches()) {
            count = Integer.parseInt(countMatcher.group(1));
            selector = countMatcher.group(2).trim();
        }
        if (count < 1) {
            throw new IllegalArgumentException("block requirement count must be positive: " + source);
        }

        Predicate<BlockState> predicate;
        if (selector.startsWith("#")) {
            ResourceLocation id = requireId(selector.substring(1), source);
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
            predicate = state -> state.is(tag);
        } else if (selector.startsWith("/") && selector.endsWith("/") && selector.length() > 2) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(selector.substring(1, selector.length() - 1));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("invalid block regex " + selector + ": " + exception.getMessage(), exception);
            }
            predicate = state -> {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                return id != null && pattern.matcher(id.toString()).find();
            };
        } else if (selector.startsWith("@")) {
            String namespace = selector.substring(1).trim().toLowerCase(Locale.ROOT);
            if (namespace.isEmpty()) {
                throw new IllegalArgumentException("empty block namespace selector: " + source);
            }
            predicate = state -> {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                return id != null && id.getNamespace().equals(namespace);
            };
        } else {
            ResourceLocation id = requireId(selector, source);
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || !ForgeRegistries.BLOCKS.containsKey(id)) {
                throw new IllegalArgumentException("unknown block id: " + id);
            }
            predicate = state -> {
                if (state.is(block)) {
                    return true;
                }
                var item = state.getBlock().asItem();
                ResourceLocation itemId = item == Items.AIR ? null : ForgeRegistries.ITEMS.getKey(item);
                return id.equals(itemId);
            };
        }
        return new BlockRequirement(predicate, count, selector);
    }

    private static ResourceLocation requireId(String value, String source) {
        ResourceLocation id = ResourceLocation.tryParse(value.trim());
        if (id == null) {
            throw new IllegalArgumentException("invalid block id in requirement: " + source);
        }
        return id;
    }

    public boolean matches(BlockState state) {
        return predicate.test(state);
    }

    public int count() {
        return count;
    }

    public String description() {
        return description;
    }

    public static BlockRequirement firstMissing(List<BlockRequirement> requirements, Level level, BlockPos center, int range) {
        if (requirements.isEmpty()) {
            return null;
        }
        int[] found = new int[requirements.size()];
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockState state = level.getBlockState(center.offset(x, y, z));
                    for (int index = 0; index < requirements.size(); index++) {
                        BlockRequirement requirement = requirements.get(index);
                        if (found[index] < requirement.count && requirement.matches(state)) {
                            found[index]++;
                        }
                    }
                }
            }
        }
        for (int index = 0; index < requirements.size(); index++) {
            if (found[index] < requirements.get(index).count) {
                return requirements.get(index);
            }
        }
        return null;
    }
}
