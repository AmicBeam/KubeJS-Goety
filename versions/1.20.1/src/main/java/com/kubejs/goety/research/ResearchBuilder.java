package com.kubejs.goety.research;

import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import dev.latvian.mods.kubejs.util.ListJS;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@Info("自定义 Goety Research 构建器")
public final class ResearchBuilder {
    private final String id;
    private ResourceLocation scrollItem;
    private boolean consumeScroll = true;
    private final List<String> prerequisites = new ArrayList<>();
    private String displayName;
    private String learnMessage;
    private String alreadyLearnedMessage;
    private String missingPrerequisiteMessage;

    public ResearchBuilder(String id) {
        this.id = id;
        this.displayName = id;
    }

    @Info(value = "绑定用于学习该研究的真实 Goety ResearchScroll 物品", params = @Param(name = "itemId", value = "使用 goety_research_scroll 类型注册的物品 ID"))
    public ResearchBuilder setScroll(String itemId) {
        this.scrollItem = itemId == null || itemId.isBlank() ? null : new ResourceLocation(itemId);
        return this;
    }

    public ResearchBuilder setConsumeScroll(boolean consumeScroll) {
        this.consumeScroll = consumeScroll;
        return this;
    }

    @Info("设置显示名称，用于提示和 tooltip")
    public ResearchBuilder setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
        return this;
    }

    @Info("添加一个前置研究")
    public ResearchBuilder requireResearch(String researchId) {
        if (researchId != null && !researchId.isBlank() && !prerequisites.contains(researchId)) {
            prerequisites.add(researchId);
        }
        return this;
    }

    @Info("设置前置研究列表；必须全部掌握")
    public ResearchBuilder setPrerequisites(Object researchIds) {
        prerequisites.clear();
        List<?> list = ListJS.of(researchIds);
        if (list != null) {
            for (Object value : list) {
                requireResearch(String.valueOf(value));
            }
        }
        return this;
    }

    public ResearchBuilder setLearnMessage(String message) {
        this.learnMessage = emptyToNull(message);
        return this;
    }

    public ResearchBuilder setAlreadyLearnedMessage(String message) {
        this.alreadyLearnedMessage = emptyToNull(message);
        return this;
    }

    public ResearchBuilder setMissingPrerequisiteMessage(String message) {
        this.missingPrerequisiteMessage = emptyToNull(message);
        return this;
    }

    public ResearchDefinition build() {
        return new ResearchDefinition(id, scrollItem, consumeScroll, prerequisites, displayName,
                learnMessage, alreadyLearnedMessage, missingPrerequisiteMessage);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
