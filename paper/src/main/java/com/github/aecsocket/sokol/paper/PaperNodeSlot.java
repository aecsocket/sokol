package com.github.aecsocket.sokol.paper;

import java.util.Set;

import com.github.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.github.aecsocket.sokol.core.OffsetNodeSlot;
import com.github.aecsocket.sokol.core.impl.BasicNodeSlot;
import com.github.aecsocket.sokol.core.rule.Rule;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public final class PaperNodeSlot extends BasicNodeSlot<
    PaperNodeSlot, PaperComponent
> implements OffsetNodeSlot {
    private final Point2 offset;

    PaperNodeSlot(PaperComponent parent, String key, Set<String> tags, Rule rule, Point2 offset) {
        super(parent, key, tags, rule);
        this.offset = offset;
    }

    private PaperNodeSlot() {
        super();
        offset = Point2.ZERO;
    }

    @Override public Point2 offset() { return offset; }
}
