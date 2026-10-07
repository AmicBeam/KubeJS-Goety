package com.kubejs.goety.research;

import com.Polarice3.Goety.common.research.Research;
import com.Polarice3.Goety.common.research.ResearchList;
import com.Polarice3.Goety.utils.SEHelper;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class ResearchData {
    private static final Map<String, ResearchDefinition> SERVER_DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, ResearchDefinition> CLIENT_DEFINITIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ResearchDefinition> SERVER_SCROLLS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ResearchDefinition> CLIENT_SCROLLS = new LinkedHashMap<>();
    private static final Set<String> CUSTOM_IDS = new HashSet<>();
    private static final Map<String, ResourceLocation> REGISTERED_SCROLLS = new LinkedHashMap<>();

    private ResearchData() {
    }

    public static synchronized void beginRegistration() {
        SERVER_DEFINITIONS.clear();
        SERVER_SCROLLS.clear();
    }

    /**
     * Called while startup item scripts are registering a real ResearchScroll.
     * This happens on both physical sides, before JEI scans the item registry.
     */
    public static synchronized Research registerScrollResearch(ResourceLocation itemId, String researchId) {
        ResourceLocation previousItem = REGISTERED_SCROLLS.putIfAbsent(researchId, itemId);
        if (previousItem != null && !previousItem.equals(itemId)) {
            throw new IllegalStateException("Research '" + researchId + "' is already bound to scroll "
                    + previousItem + "; cannot also bind " + itemId);
        }

        Research research = ResearchList.getResearch(researchId);
        if (research == null) {
            research = new Research(researchId);
            ResearchList.registerResearch(researchId, research);
            CUSTOM_IDS.add(researchId);
        } else if (!CUSTOM_IDS.contains(researchId)) {
            throw new IllegalStateException("Research ID '" + researchId
                    + "' is already registered by Goety or another mod");
        }
        return research;
    }

    public static synchronized boolean register(ResearchDefinition definition) {
        String id = definition.id();
        for (String prerequisite : definition.prerequisites()) {
            if (prerequisite.equals(id)) {
                ScriptType.SERVER.console.error("Research '" + id + "' cannot require itself");
                return false;
            }
        }

        Research existing = ResearchList.getResearch(id);
        if (existing == null) {
            ResearchList.registerResearch(id, new Research(id));
            CUSTOM_IDS.add(id);
        } else if (!CUSTOM_IDS.contains(id)) {
            ScriptType.SERVER.console.error("Research ID '" + id + "' is already registered by Goety or another mod and cannot be replaced");
            return false;
        }

        if (definition.scrollItem() != null) {
            if (!BuiltInRegistries.ITEM.containsKey(definition.scrollItem())) {
                ScriptType.SERVER.console.error("Research '" + id + "' references missing scroll item "
                        + definition.scrollItem());
                return false;
            }
            Item item = BuiltInRegistries.ITEM.get(definition.scrollItem());
            if (!(item instanceof com.Polarice3.Goety.common.items.research.ResearchScroll scroll)) {
                ScriptType.SERVER.console.error("Research scroll " + definition.scrollItem()
                        + " must be registered with item type 'goety_research_scroll'");
                return false;
            }
            if (scroll.research != existing) {
                ScriptType.SERVER.console.error("Research scroll " + definition.scrollItem()
                        + " targets '" + scroll.research.getId() + "', but definition targets '" + id + "'");
                return false;
            }
        }

        ResearchDefinition previousDefinition = SERVER_DEFINITIONS.put(id, definition);
        if (previousDefinition != null && previousDefinition.scrollItem() != null
                && SERVER_SCROLLS.get(previousDefinition.scrollItem()) == previousDefinition) {
            SERVER_SCROLLS.remove(previousDefinition.scrollItem());
        }
        if (definition.scrollItem() != null) {
            ResearchDefinition conflict = SERVER_SCROLLS.put(definition.scrollItem(), definition);
            if (conflict != null && !conflict.id().equals(id)) {
                ScriptType.SERVER.console.warn("Research scroll " + definition.scrollItem()
                        + " was reassigned from '" + conflict.id() + "' to '" + id + "'");
            }
        }
        return true;
    }

    public static synchronized void validateDefinitions() {
        for (ResearchDefinition definition : SERVER_DEFINITIONS.values()) {
            for (String prerequisite : definition.prerequisites()) {
                if (ResearchList.getResearch(prerequisite) == null) {
                    ScriptType.SERVER.console.warn("Research '" + definition.id()
                            + "' has unknown prerequisite '" + prerequisite + "'");
                }
            }
        }
    }

    public static synchronized Collection<ResearchDefinition> serverDefinitions() {
        return List.copyOf(SERVER_DEFINITIONS.values());
    }

    public static synchronized void installClientDefinitions(Collection<ResearchDefinition> definitions) {
        CLIENT_DEFINITIONS.clear();
        CLIENT_SCROLLS.clear();
        for (ResearchDefinition definition : definitions) {
            CLIENT_DEFINITIONS.put(definition.id(), definition);
            if (definition.scrollItem() != null) {
                CLIENT_SCROLLS.put(definition.scrollItem(), definition);
            }
            if (ResearchList.getResearch(definition.id()) == null) {
                ResearchList.registerResearch(definition.id(), new Research(definition.id()));
                CUSTOM_IDS.add(definition.id());
            }
        }
    }

    @Nullable
    public static synchronized ResearchDefinition definitionForScroll(ResourceLocation itemId, boolean clientSide) {
        return (clientSide ? CLIENT_SCROLLS : SERVER_SCROLLS).get(itemId);
    }

    @Nullable
    public static synchronized ResearchDefinition definition(String id, boolean clientSide) {
        return (clientSide ? CLIENT_DEFINITIONS : SERVER_DEFINITIONS).get(id);
    }

    @Nullable
    public static Research getResearch(String id) {
        return id == null ? null : ResearchList.getResearch(id);
    }

    public static boolean has(Player player, String id) {
        Research research = getResearch(id);
        return player != null && research != null && SEHelper.hasResearch(player, research);
    }

    public static boolean grant(Player player, String id) {
        Research research = getResearch(id);
        if (player == null || research == null) {
            return false;
        }
        return SEHelper.addResearch(player, research);
    }

    public static boolean revoke(Player player, String id) {
        Research research = getResearch(id);
        if (player == null || research == null) {
            return false;
        }
        return SEHelper.removeResearch(player, research);
    }

    public static List<String> getResearchIds(Player player) {
        List<String> ids = new ArrayList<>();
        if (player != null) {
            for (Research research : SEHelper.getResearch(player)) {
                if (research != null) {
                    ids.add(research.getId());
                }
            }
        }
        return ids;
    }

    public static List<String> missingPrerequisites(Player player, ResearchDefinition definition) {
        List<String> missing = new ArrayList<>();
        for (String prerequisite : definition.prerequisites()) {
            if (!has(player, prerequisite)) {
                missing.add(prerequisite);
            }
        }
        return missing;
    }

}
