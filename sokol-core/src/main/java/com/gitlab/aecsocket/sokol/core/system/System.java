package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface System<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {
    interface Instance<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {
        @NotNull N parent();
        @NotNull System<N> base();
        @NotNull SokolPlatform platform();

        void build();
    }

    @NotNull String id();
    @NotNull Map<String, Stat<?>> baseStats();
    @NotNull Map<String, Class<? extends Rule>> ruleTypes();
    @NotNull Instance<N> create(N node, Component component);
}
