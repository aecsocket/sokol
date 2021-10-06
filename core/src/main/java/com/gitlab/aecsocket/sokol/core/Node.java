package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

public interface Node {
    Component value();

    @Nullable Node parent();
    @Nullable String key();

    NodePath path();
    Node root();

    Map<String, ? extends Node> nodes();
    Optional<? extends Node> node(String... path);
    Node removeNode(String key);

    Map<String, ? extends Feature<?>> features();
    Optional<? extends Feature<?>> feature(String key);

    Node copy();

    interface Scoped<
            N extends Scoped<N, C, F>,
            C extends Component.Scoped<C, ?, ? extends FeatureType<? extends F, N>, N>,
            F extends Feature.Scoped<? extends F, N>
    > extends Node {
        N self();

        @Override C value();

        @Override @Nullable N parent();
        N parent(N parent, String key);
        N orphan();

        @Override N root();

        @Override Map<String, N> nodes();
        @Override Optional<N> node(String... path);
        @Override N removeNode(String key);
        N node(String key, N val) throws IncompatibilityException;

        @Override Map<String, F> features();
        @Override Optional<F> feature(String key);

        @Override N copy();
    }
}
