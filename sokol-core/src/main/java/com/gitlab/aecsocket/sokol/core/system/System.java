package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public interface System {
    interface Instance {
        @NotNull TreeNode parent();
        @NotNull System base();
        @NotNull SokolPlatform platform();

        default void build() {}
    }

    @NotNull String id();
    default @NotNull Map<String, Stat<?>> baseStats() { return Collections.emptyMap(); }
    default @NotNull Map<String, Class<? extends Rule>> ruleTypes() { return Collections.emptyMap(); }
    @NotNull Instance create(TreeNode node, Component component);
}
