package com.kubejs.goety.event;

import com.kubejs.goety.research.ResearchBuilder;
import com.kubejs.goety.research.ResearchData;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.typings.Generics;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;

import java.util.function.Consumer;
import java.util.regex.Pattern;

@Info("注册自定义 Goety Research，并绑定真实的 Goety ResearchScroll 物品")
public final class RegisterResearchEventJS extends EventJS {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_./-]+");

    @Info(value = "创建自定义 Research", params = {
            @Param(name = "researchId", value = "不带命名空间的 Research ID"),
            @Param(name = "builder", value = "Research 配置函数")
    })
    @Generics(ResearchBuilder.class)
    public void create(String researchId, Consumer<ResearchBuilder> builder) {
        if (researchId == null || !VALID_ID.matcher(researchId).matches()) {
            ScriptType.SERVER.console.error("Invalid Research ID '" + researchId
                    + "'. Use lowercase path characters and do not include a namespace");
            return;
        }

        try {
            ResearchBuilder researchBuilder = new ResearchBuilder(researchId);
            if (builder != null) {
                builder.accept(researchBuilder);
            }
            if (ResearchData.register(researchBuilder.build())) {
                ScriptType.SERVER.console.info("✓ Registered Research: " + researchId);
            }
        } catch (Exception e) {
            ScriptType.SERVER.console.error("Failed to register Research '" + researchId + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
}
