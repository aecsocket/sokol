package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.api.Component;
import com.github.aecsocket.sokol.core.api.OffsetNodeSlot;
import com.github.aecsocket.sokol.core.impl.AbstractNodeSlot;
import com.github.aecsocket.sokol.core.rule.base.BaseRule;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;

import java.util.Set;

public final class PaperNodeSlot
        extends AbstractNodeSlot implements OffsetNodeSlot {
    private final Point2 offset;

    public PaperNodeSlot(Component parent, String key, Set<String> tags, BaseRule rule, Point2 offset) {
        super(parent, key, tags, rule);
        this.offset = offset;
    }

    @Override public Point2 offset() { return offset; }
}
