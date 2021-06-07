package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.System;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface TreeNode {
    @NotNull Component value();

    @NotNull Map<String, ? extends TreeNode> children();
    @Nullable TreeNode child(String key);

    @NotNull Map<String, ? extends System<?>> systems();
    @Nullable System<?> system(String id);

    @Nullable String key();
    @Nullable TreeNode parent();
}
