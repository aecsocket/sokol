package com.github.aecsocket.sokol.core.nodeview;

import com.github.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.github.aecsocket.sokol.core.NodeSlot;
import com.github.aecsocket.sokol.core.SokolComponent;
import com.github.aecsocket.sokol.core.TreeNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.function.Consumer;

public record NodeView<
    N extends TreeNode.Scoped<N, ?, ? extends SokolComponent.Scoped<?, S, ?>, ?, ?>,
    S extends NodeSlot.Scoped<S, ?>
>(
    Options options,
    N root,
    int amount,
    Consumer<N> callback
) {
    @ConfigSerializable
    public record Options(
        boolean modifiable,
        boolean limited,
        Point2 center
    ) {
        public static final Options DEFAULT = new Options(true, true, Point2.point2(4, 3));

        public Options() {
            this(true, true, Point2.point2(4, 3));
        }
    }
}
