package com.github.aecsocket.sokol.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.MapNode;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.context.Context;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface BlueprintNode extends SokolNode {
    Map<String, ? extends FeatureData<?, ?, ?>> featureData();

    TreeNode asTreeNode(Context context);


    @Override @Nullable BlueprintNode parent();
    @Override BlueprintNode root();

    @Override Map<String, ? extends BlueprintNode> children();
    @Override Collection<? extends BlueprintNode> childValues();

    @Override Optional<? extends BlueprintNode> get(NodePath path);
    @Override Optional<? extends BlueprintNode> get(String... path);
    @Override Optional<? extends BlueprintNode> get(String path);

    void visitBlueprints(Consumer<BlueprintNode> visitor);

    @Override BlueprintNode copy();
    @Override BlueprintNode asRoot();

    interface Scoped<
        B extends Scoped<B, N, C, F>,
        N extends TreeNode.Scoped<N, B, C, ?, ?>,
        C extends SokolComponent.Scoped<C, ?, ?>,
        F extends FeatureData<?, ?, N>
    > extends BlueprintNode, MapNode.Scoped<B> {
        @Override Map<String, F> featureData();
        @Override Optional<F> featureData(String key);

        @Override N asTreeNode(Context context);
    }
}
