package com.github.aecsocket.sokol.core.api;

import com.gitlab.aecsocket.minecommons.core.node.MapNode;

import java.util.Set;

public interface BaseNode extends MapNode {
    Component value();

    Set<String> featureKeys();
    boolean hasFeature(String key);

    interface Scoped<
            N extends Scoped<N, C>,
            C extends Component.Scoped<C, ?, ?>
    > extends BaseNode, MapNode.Scoped<N> {
        @Override C value();
    }
}
