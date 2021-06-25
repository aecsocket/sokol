package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

public class BasicTreeNode<C extends Component.Scoped<C, S, B>, S extends Slot, B extends System, Y extends System.Instance>
        extends AbstractTreeNode<BasicTreeNode<C, S, B, Y>, C, S, B, Y> {
    public BasicTreeNode(C value) {
        super(value);
    }

    @Override protected BasicTreeNode<C, S, B, Y> self() { return this; }

    @Override
    public @NotNull BasicTreeNode<C, S, B, Y> asRoot() {
        BasicTreeNode<C, S, B, Y> result = new BasicTreeNode<>(value);
        result.children.putAll(children);
        return result;
    }
}
