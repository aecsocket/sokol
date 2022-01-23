package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.node.MapNode;
import com.gitlab.aecsocket.minecommons.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.context.Context;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface Blueprint extends MapNode {
    Component value();
    Map<String, ?> featureData();

    Node build(Context context);

    @Override @Nullable Blueprint parent();
    @Override Blueprint root();

    @Override Map<String, ? extends Blueprint> children();
    @Override Collection<? extends Blueprint> childValues();

    @Override Optional<? extends Blueprint> get(NodePath path);
    @Override Optional<? extends Blueprint> get(String... path);

    void visitBlueprints(Consumer<Blueprint> visitor);

    Blueprint copy();
    Blueprint asRoot();

    interface Scoped<
            B extends Scoped<B, C, N>,
            C extends Component.Scoped<C, ?, ?, N>,
            N extends Node.Scoped<N, ?, C, ?>
    > extends Blueprint, MapNode.Scoped<B> {
        @Override C value();
        @Override N build(Context context);
    }

    interface Mutable<
            B extends Mutable<B, C, N>,
            C extends Component.Scoped<C, ?, ?, N>,
            N extends Node.Scoped<N, ?, C, ?>
    > extends Scoped<B, C, N>, MapNode.Mutable<B> {}
}
