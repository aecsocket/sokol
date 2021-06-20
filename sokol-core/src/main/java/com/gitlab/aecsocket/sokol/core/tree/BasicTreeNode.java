package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.system.System;

public class BasicTreeNode<C extends Component.Scoped<C, S, B>, S extends Slot, B extends System<BasicTreeNode<C, S, B, Y>>, Y extends System.Instance<BasicTreeNode<C, S, B, Y>>>
        extends AbstractTreeNode<BasicTreeNode<C, S, B, Y>, C, S, B, Y> {
    public BasicTreeNode(C value, EventDispatcher<TreeEvent> events, StatMap stats, String key, BasicTreeNode<C, S, B, Y> parent) {
        super(value, events, stats, key, parent);
    }

    public BasicTreeNode(C value, EventDispatcher<TreeEvent> events, StatMap stats) {
        super(value, events, stats);
    }

    public BasicTreeNode(C value, BasicTreeNode<C, S, B, Y> o) {
        super(value, o);
    }

    public BasicTreeNode(C value) {
        super(value);
    }

    @Override protected BasicTreeNode<C, S, B, Y> self() { return this; }
}
