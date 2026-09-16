package com.kubejs.goety.research;

import com.Polarice3.Goety.common.items.research.ResearchScroll;
import com.Polarice3.Goety.common.research.Research;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.regex.Pattern;

/**
 * A real Goety ResearchScroll registered by a KubeJS startup item builder.
 * Keeping Goety's concrete item type is important because Goety's JEI plugin
 * discovers research scrolls with an instanceof ResearchScroll check.
 */
public final class CustomResearchScrollItem extends ResearchScroll {
    private final Builder builder;
    private final String researchId;

    private CustomResearchScrollItem(Builder builder, Research research) {
        super(builder.createItemProperties(), research);
        this.builder = builder;
        this.researchId = builder.researchId;
    }

    public String getResearchId() {
        return researchId;
    }

    @Override
    public Component researchGet() {
        ResearchDefinition definition = ResearchData.definition(researchId, false);
        if (definition != null && definition.learnMessage() != null) {
            return Component.literal(definition.learnMessage());
        }
        return definition == null ? null
                : Component.translatable("message.kubejs_goety.research.learned", definition.displayName());
    }

    public ItemBuilder kjs$getItemBuilder() {
        return builder;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (builder.displayName != null && builder.formattedDisplayName) {
            return builder.displayName;
        }
        return super.getName(stack);
    }

    public static final class Builder extends ItemBuilder {
        private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_./-]+");
        private String researchId;

        public Builder(ResourceLocation id) {
            super(id);
            maxStackSize = 1;
            rarity = Rarity.EPIC;
        }

        @Info(value = "设置该 Goety 研究卷轴对应的 Research ID", params = {
                @Param(name = "researchId", value = "不带命名空间，例如 ancient_magic")
        })
        public Builder research(String researchId) {
            if (researchId == null || !VALID_ID.matcher(researchId).matches()) {
                throw new IllegalArgumentException("Research ID must use lowercase path characters and must not "
                        + "contain a namespace: " + researchId);
            }
            this.researchId = researchId;
            return this;
        }

        @Override
        public Item createObject() {
            if (researchId == null) {
                throw new IllegalStateException("Goety research scroll " + id
                        + " is missing .research('research_id')");
            }
            Research research = ResearchData.registerScrollResearch(id, researchId);
            return new CustomResearchScrollItem(this, research);
        }
    }
}
