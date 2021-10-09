package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.RuleException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

public interface Node {
    Component value();

    @Nullable Node parent();
    @Nullable String key();

    NodePath path();
    Node root();
    boolean isRoot();

    Map<String, ? extends Node> nodes();
    Optional<? extends Node> node(String... path);
    Optional<? extends Node> node(NodePath path);
    Node removeNode(String key);

    Map<String, ? extends FeatureInstance<?>> features();
    Optional<? extends FeatureInstance<?>> feature(String key);

    EventDispatcher<? extends NodeEvent<?>> events();
    StatMap stats();

    Node copy();
    Node asRoot();

    interface Scoped<
            N extends Scoped<N, C, F>,
            C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
            F extends FeatureInstance<N>
    > extends Node {
        N self();

        @Override C value();

        @Override @Nullable N parent();
        N parent(N parent, String key);
        N orphan();

        @Override N root();

        @Override Map<String, N> nodes();
        @Override Optional<N> node(String... path);
        @Override Optional<N> node(NodePath path);
        @Override N removeNode(String key);
        N node(String key, N val) throws RuleException;

        @Override Map<String, F> features();
        @Override Optional<F> feature(String key);

        @Override EventDispatcher<NodeEvent<N>> events();
        void build(NodeEvent<N> event);

        @Override N copy();
        @Override N asRoot();
    }
}
