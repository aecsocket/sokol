package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public interface Node {
    Component value();

    @Nullable Node parent();
    @Nullable String key();

    NodePath path();
    Node root();
    boolean isRoot();

    Map<String, ? extends Node> nodes();
    Set<String> nodeKeys();
    Collection<? extends Node> nodeValues();
    Optional<? extends Node> node(String... path);
    Optional<? extends Node> node(NodePath path);
    Node removeNode(String key);

    Map<String, ? extends FeatureInstance<?>> features();
    Set<String> featureKeys();
    Collection<? extends FeatureInstance<?>> featureValues();
    Optional<? extends FeatureInstance<?>> feature(String key);

    TreeContext<?> build(Locale locale);
    TreeContext<?> build(ItemUser user);

    Item createItem(Locale locale) throws ItemCreationException;
    Item createItem(ItemUser user) throws ItemCreationException;

    Node copy();
    Node asRoot();

    interface Scoped<
            N extends Scoped<N, I, C, F>,
            I extends Item.Scoped<I, N>,
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
        @Override Collection<N> nodeValues();
        @Override Optional<N> node(String... path);
        @Override Optional<N> node(NodePath path);
        @Override N removeNode(String key);
        N node(String key, N val, TreeContext<N> ctx , TreeContext<N> valCtx) throws IncompatibilityException;

        @Override Map<String, F> features();
        @Override Collection<F> featureValues();
        @Override Optional<F> feature(String key);
        N feature(String key, F feature);

        @Override TreeContext<N> build(Locale locale);
        @Override TreeContext<N> build(ItemUser user);

        @Override I createItem(Locale locale) throws ItemCreationException;
        @Override I createItem(ItemUser user) throws ItemCreationException;

        @Override N copy();
        @Override N asRoot();
    }
}
