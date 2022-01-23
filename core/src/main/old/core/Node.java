package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.node.MapNode;
import com.gitlab.aecsocket.minecommons.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public interface Node extends MapNode {
    Component value();
    TreeData<?> tree();

    Map<String, ? extends FeatureInstance<?, ?>> features();
    Set<String> featureKeys();
    Collection<? extends FeatureInstance<?, ?>> featureValues();
    Optional<? extends FeatureInstance<?, ?>> feature(String key);

    Item createItem() throws ItemCreationException;

    Blueprint asBlueprint();

    @Nullable Node parent();
    @Override Node root();

    @Override Map<String, ? extends Node> children();
    @Override Collection<? extends Node> childValues();
    @Override Optional<? extends Node> get(NodePath path);
    @Override Optional<? extends Node> get(String... path);

    @Override Node copy();
    @Override Node asRoot();

    interface Scoped<
            N extends Scoped<N, I, C, F, B>,
            I extends Item.Scoped<I, N>,
            C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
            F extends FeatureInstance<?, N>,
            B extends Blueprint.Mutable<B, C, N>
    > extends Node {
        @Override C value();
        @Override TreeData<N> tree();

        @Override Map<String, F> features();
        @Override Collection<F> featureValues();
        @Override Optional<F> feature(String key);

        @Override I createItem() throws ItemCreationException;

        @Override B asBlueprint();
    }

    interface Mutable<
            N extends Mutable<N, I, C, F, B>,
            I extends Item.Scoped<I, N>,
            C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
            F extends FeatureInstance<?, N>,
            B extends Blueprint.Mutable<B, C, N>
    > extends Scoped<N, I, C, F, B>, MapNode.Mutable<N> {}
}
