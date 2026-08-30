package com.kubejs.goety.util;

import com.Polarice3.Goety.common.research.Research;
import com.Polarice3.Goety.common.research.ResearchList;
import com.Polarice3.Goety.utils.SEHelper;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Goety 研究辅助工具
 * <p>
 * 提供自定义研究的注册，以及玩家研究状态的查询与授予。
 * 对应 Goety 原版机制：
 * - 研究(Research)只是一个 ID 字符串，注册进 {@link ResearchList} 的静态列表
 * - 玩家存档只保存研究 ID 字符串，读档时通过 {@link ResearchList#getResearch(String)} 反查，
 *   因此自定义研究必须在玩家读档前注册（ServerEvents.LOADED 时机满足此要求）
 */
public class ResearchHelper {
    // 用 Log4j 而非 ScriptType.SERVER.console：注册研究可能在 startup 阶段
    // （脚本顶层 / 物品创建时）被调用，此时 SERVER console 尚不可用
    private static final Logger LOGGER = LogManager.getLogger("KubeJS-Goety");

    /**
     * 注册一个新研究
     *
     * @param researchId 研究 ID，建议使用带命名空间的形式（如 'kubejs:my_research'），
     *                   以避开 Goety 内置研究（forbidden、ravaging 等）
     * @return 注册的研究对象，ID 非法时返回 null
     */
    public static Research registerResearch(String researchId) {
        if (researchId == null || researchId.isEmpty()) {
            LOGGER.error("Research ID cannot be empty");
            return null;
        }
        Research existing = ResearchList.getResearch(researchId);
        if (existing != null) {
            LOGGER.warn("Research '{}' already exists, will be overwritten (make sure it is only registered once!)", researchId);
        }
        Research research = new Research(researchId);
        ResearchList.registerResearch(researchId, research);
        return research;
    }

    /**
     * 获取已注册的研究对象
     * <p>
     * 注意：SEHelper 内部通过 List.contains 比较 Research，而 Research 未重写 equals，
     * 实际为引用比较——授予/查询时必须使用本方法返回的同一实例
     *
     * @param researchId 研究 ID
     * @return 研究对象，未注册时返回 null
     */
    public static Research getResearch(String researchId) {
        return ResearchList.getResearch(researchId);
    }

    /**
     * 查询玩家是否已习得指定研究
     *
     * @param player     MC 的 Player 对象（如 ItemEvents.rightClicked 事件中的 player 直接就是）
     * @param researchId 研究 ID
     * @return 已习得返回 true；研究未注册视为未习得
     */
    public static boolean hasResearch(Player player, String researchId) {
        Research research = getResearch(researchId);
        return research != null && SEHelper.hasResearch(player, research);
    }

    /**
     * 授予玩家指定研究
     *
     * @param player     MC 的 Player 对象（如 ItemEvents.rightClicked 事件中的 player 直接就是）
     * @param researchId 研究 ID
     * @return 授予成功返回 true；研究未注册或玩家已习得时返回 false
     */
    public static boolean grantResearch(Player player, String researchId) {
        Research research = getResearch(researchId);
        if (research == null) {
            LOGGER.error("Research '{}' is not registered", researchId);
            return false;
        }
        return SEHelper.addResearch(player, research);
    }
}
