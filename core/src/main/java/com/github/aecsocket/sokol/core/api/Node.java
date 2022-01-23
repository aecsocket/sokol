package com.github.aecsocket.sokol.core.api;

import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.world.ItemStack;
import com.gitlab.aecsocket.minecommons.core.node.MapNode;
import com.gitlab.aecsocket.minecommons.core.node.NodePath;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface Node extends BaseNode {
    Context context();
    Tree<?> tree();

    Map<String, ? extends FeatureInstance<?, ?, ?>> features();
    Optional<? extends FeatureInstance<?, ?, ?>> feature(String key);

    Blueprint asBlueprint();
    ItemStack asItem() throws ItemCreationException;


    @Override @Nullable Node parent();
    @Override Node root();

    @Override Map<String, ? extends Node> children();
    @Override Collection<? extends Node> childValues();

    @Override Optional<? extends Node> get(NodePath path);
    @Override Optional<? extends Node> get(String... path);

    void visitNodes(Consumer<Node> visitor);

    @Override Node copy();
    @Override Node asRoot();

    interface Scoped<
            N extends Scoped<N, B, C, I, S>,
            B extends Blueprint.Scoped<B, N, C, ?>,
            C extends Component.Scoped<C, ?, ? extends FeatureConfig<?, N, C, ? extends FeatureData<?, N, ?, I>>>,
            I extends FeatureInstance<?, N, ?>,
            S extends ItemStack.Scoped<S, B>
    > extends Node, BaseNode.Scoped<N, C> {
        @Override C value();
        @Override Tree<N> tree();

        @Override Map<String, I> features();
        @Override Optional<I> feature(String key);

        @Override B asBlueprint();
        @Override S asItem();
    }

    interface Mutable<
            N extends Mutable<N, B, C, I, S>,
            B extends Blueprint.Scoped<B, N, C, ?>,
            C extends Component.Scoped<C, ?, ? extends FeatureConfig<?, N, C, ? extends FeatureData<?, N, ?, I>>>,
            I extends FeatureInstance<?, N, ?>,
            S extends ItemStack.Scoped<S, B>
    > extends Scoped<N, B, C, I, S>, MapNode.Mutable<N> {
        void tree(Tree<N> tree);
        void build();
    }
}
