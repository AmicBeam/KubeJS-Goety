package com.kubejs.goety.item;

import com.Polarice3.Goety.common.items.research.Scroll;
import com.Polarice3.Goety.common.research.Research;
import com.kubejs.goety.util.ResearchHelper;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KubeJS 自定义物品类型：Goety 研究卷轴
 * <p>
 * 允许脚本注册真正的 {@link Scroll} 类物品（类型名 kubejs_goety:scroll）：
 * <pre>
 * StartupEvents.registry('item', event =&gt; {
 *     event.create('kubejs:my_scroll', 'kubejs_goety:scroll')
 *         .research('kubejs:my_research')   // 需先注册（见下）
 *         .texture('goety:item/old_research_scroll');
 * })
 * </pre>
 * <p>
 * 与「普通 KubeJS 物品 + 脚本模拟右键」相比，此方案的必要性：
 * <ul>
 *   <li>右键学习研究、消耗物品由 Goety 原生实现（{@link Scroll#use}），无需脚本处理；
 *       习得消息走 lang 键 {@code info.goety.research.<researchId>}</li>
 *   <li>tooltip 原生显示研究介绍（{@code info.goety.items.<researchId>}）与提示
 *       （{@code info.goety.items.scroll}）</li>
 *   <li>JEI 仪式配方页只展示 {@code ResearchScroll} 子类物品（引用比较 research 实例），
 *       普通 KubeJS Item 永远无法显示——只有真正的 Scroll 类物品才会出现</li>
 * </ul>
 * <p>
 * 时序要求：研究必须在物品创建（createObject）之前注册。
 * 推荐把 {@code goetyResearch.registerResearch('kubejs:my_research')} 写在 startup 脚本
 * 顶层（脚本加载早于 registry 事件中的 createObject），注册进 ResearchList 后
 * 物品创建时按 ID 反查同一实例，保证 JEI 的引用比较通过。
 */
public class ScrollItemBuilder extends ItemBuilder {
    private static final Logger LOGGER = LogManager.getLogger("KubeJS-Goety");

    /**
     * 所有已成功创建的卷轴所绑定的研究 ID（去重）。
     * 插件监听 ClientEvents.LANG 时据此为每个研究自动添加默认语言键
     * （info.goety.research.&lt;id&gt; 习得消息、info.goety.items.&lt;id&gt; 介绍），
     * 按当前语言提供中/英默认文案——脚本用户无需自行写任何 lang。
     */
    private static final Set<String> TRACKED_RESEARCH_IDS = ConcurrentHashMap.newKeySet();

    /** 已跟踪的研究 ID（供插件注册默认语言键） */
    public static Set<String> getTrackedResearchIds() {
        return TRACKED_RESEARCH_IDS;
    }

    private String researchId;
    private boolean consumable = true;

    public ScrollItemBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * 绑定此卷轴对应的研究 ID（需先经 ResearchHelper 注册，否则物品创建时会回退为普通 Item 并报错）
     */
    public ScrollItemBuilder research(String researchId) {
        this.researchId = researchId;
        return this;
    }

    /**
     * 习得成功后是否消耗物品（默认 true，与 Goety 原版一致；false 则卷轴可反复使用）
     */
    public ScrollItemBuilder consumable(boolean consumable) {
        this.consumable = consumable;
        return this;
    }

    @Override
    public Item createObject() {
        Research research = ResearchHelper.getResearch(researchId);
        if (research == null) {
            LOGGER.error("Scroll item '{}' references unregistered research '{}'! "
                            + "Call goetyResearch.registerResearch('{}') at the top level of a startup script (before the item is created).",
                    id, researchId, researchId);
            return new Item(createItemProperties());
        }
        TRACKED_RESEARCH_IDS.add(research.getId());
        return new KubeJSScroll(createItemProperties(), research, consumable);
    }
}
