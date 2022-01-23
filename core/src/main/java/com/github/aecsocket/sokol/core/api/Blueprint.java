package com.github.aecsocket.sokol.core.api;

import com.github.aecsocket.sokol.core.context.Context;
import com.gitlab.aecsocket.minecommons.core.node.MapNode;
import com.gitlab.aecsocket.minecommons.core.node.NodePath;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface Blueprint extends BaseNode {
    Map<String, ? extends FeatureData<?, ?, ?, ?>> features();
    Optional<? extends FeatureData<?, ?, ?, ?>> feature(String key);

    Node asNode(Context context);


    @Override @Nullable Blueprint parent();
    @Override Blueprint root();

    @Override Map<String, ? extends Blueprint> children();
    @Override Collection<? extends Blueprint> childValues();

    @Override Optional<? extends Blueprint> get(NodePath path);
    @Override Optional<? extends Blueprint> get(String... path);

    void visitBlueprints(Consumer<Blueprint> visitor);

    @Override Blueprint copy();
    @Override Blueprint asRoot();

    interface Scoped<
            B extends Scoped<B, N, C, D>,
            N extends Node.Scoped<N, B, C, ?, ?>,
            C extends Component.Scoped<C, ?, ? extends FeatureConfig<?, N, C, D>>,
            D extends FeatureData<?, N, B, ?>
    > extends Blueprint, BaseNode.Scoped<B, C> {
        @Override C value();

        @Override Map<String, D> features();
        @Override Optional<D> feature(String key);

        @Override N asNode(Context context);
    }

    interface Mutable<
            B extends Mutable<B, N, C, D>,
            N extends Node.Scoped<N, B, C, ?, ?>,
            C extends Component.Scoped<C, ?, ? extends FeatureConfig<?, N, C, D>>,
            D extends FeatureData<?, N, B, ?>
    > extends Scoped<B, N, C, D>, MapNode.Mutable<B> {}
}
