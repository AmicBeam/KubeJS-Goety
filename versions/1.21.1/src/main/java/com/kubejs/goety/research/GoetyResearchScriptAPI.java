package com.kubejs.goety.research;

import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.world.entity.player.Player;

import java.util.List;

@Info("查询和修改玩家的 Goety Research")
public final class GoetyResearchScriptAPI {
    public static final GoetyResearchScriptAPI INSTANCE = new GoetyResearchScriptAPI();

    private GoetyResearchScriptAPI() {
    }

    public boolean has(Player player, String researchId) {
        return ResearchData.has(player, researchId);
    }

    public boolean grant(Player player, String researchId) {
        return ResearchData.grant(player, researchId);
    }

    public boolean revoke(Player player, String researchId) {
        return ResearchData.revoke(player, researchId);
    }

    public List<String> getAll(Player player) {
        return ResearchData.getResearchIds(player);
    }
}
