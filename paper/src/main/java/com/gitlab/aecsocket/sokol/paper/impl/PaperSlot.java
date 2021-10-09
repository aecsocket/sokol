package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.impl.BasicSlot;
import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.rule.Rule;

import java.util.Set;

public class PaperSlot extends BasicSlot {
    private final Point2 offset;

    public PaperSlot(Component parent, String key, Set<String> tags, Rule rule, Point2 offset) {
        super(parent, key, tags, rule);
        this.offset = offset;
    }

    public Point2 offset() { return offset; }
}
