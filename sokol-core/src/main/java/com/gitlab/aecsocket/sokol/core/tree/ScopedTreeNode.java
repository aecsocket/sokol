package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.System;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ScopedTreeNode<N extends ScopedTreeNode<N, C, B, Y>, C extends Component.Scoped<C, ?, B>, B extends System.Base<Y>, Y extends System<B>>
        extends TreeNode {
    @NotNull C value();

    @Override @NotNull Map<String, N> children();
    @Override @Nullable N child(String key);
    void child(String key, N child);

    @Override @NotNull Map<String, Y> systems();
    @Override @Nullable Y system(String id);
    void system(Y system);

    @Override @Nullable N parent();
}
