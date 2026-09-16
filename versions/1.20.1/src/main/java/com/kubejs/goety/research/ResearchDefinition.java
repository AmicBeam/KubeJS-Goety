package com.kubejs.goety.research;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResearchDefinition(
        String id,
        @Nullable ResourceLocation scrollItem,
        boolean consumeScroll,
        List<String> prerequisites,
        String displayName,
        @Nullable String learnMessage,
        @Nullable String alreadyLearnedMessage,
        @Nullable String missingPrerequisiteMessage
) {
    public ResearchDefinition {
        prerequisites = List.copyOf(prerequisites);
    }
}
