package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PaperNode extends AbstractNode<PaperNode, PaperComponent, PaperFeature<?>> {
    public PaperNode(PaperComponent value, @Nullable PaperNode parent, @Nullable String key) {
        super(value, parent, key);
    }

    public PaperNode(PaperComponent value) {
        super(value);
    }

    public PaperNode(PaperNode o) {
        super(o);
    }

    @Override public PaperNode self() { return this; }

    @Override
    public PaperNode copy() {
        return new PaperNode(this);
    }
}
