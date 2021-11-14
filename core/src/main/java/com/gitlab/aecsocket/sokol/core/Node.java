package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface Node {
    Component value();

    @Nullable Node parent();
    @Nullable String key();

    NodePath path();
    Node root();
    boolean isRoot();
    @Nullable Optional<? extends TreeData<?>> treeData();

    Map<String, ? extends Node> nodes();
    Optional<? extends Node> node(String... path);
    Optional<? extends Node> node(NodePath path);
    Node removeNode(String key);

    Map<String, ? extends FeatureInstance<?>> features();
    Optional<? extends FeatureInstance<?>> feature(String key);

    Item createItem(Locale locale) throws ItemCreationException;

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
        @Override @Nullable Optional<TreeData.Scoped<N>> treeData();

        @Override Map<String, N> nodes();
        @Override Optional<N> node(String... path);
        @Override Optional<N> node(NodePath path);
        @Override N removeNode(String key);
        N node(String key, N val) throws IncompatibilityException;

        @Override Map<String, F> features();
        @Override Optional<F> feature(String key);
        N feature(String key, F feature);

        @Override N copy();
        @Override N asRoot();

        <E extends NodeEvent<N>> E call(E event);
    }
}
