package old.impl;

import com.github.aecsocket.sokol.core.api.Component;
import com.github.aecsocket.sokol.core.impl.BasicSlot;
import com.github.aecsocket.sokol.core.nodeview.OffsetSlot;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Set;

@ConfigSerializable
public final class PaperSlot extends BasicSlot implements OffsetSlot {
    private final Point2 offset;

    public PaperSlot(Component parent, String key, Set<String> tags, NodeRule rule, Point2 offset) {
        super(parent, key, tags, rule);
        this.offset = offset;
    }

    private PaperSlot() {
        super();
        offset = Point2.ZERO;
    }

    @Override public Point2 offset() { return offset; }

    @Override
    public String toString() {
        return "PaperSlot:" + key + '{' +
                "tags=" + tags +
                ", rule=" + rule +
                ", offset=" + offset +
                '}';
    }
}
