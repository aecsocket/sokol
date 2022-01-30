package com.github.aecsocket.sokol.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.MapNode;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface BlueprintNode extends SokolNode {
    SokolComponent component();

    Map<String, ? extends FeatureData<?, ?, ?>> features();
    Optional<? extends FeatureData<?, ?, ?>> feature(String key);

    TreeNode asTreeNode(Context context);


    @Override @Nullable BlueprintNode parent();
    @Override BlueprintNode root();

    @Override Map<String, ? extends BlueprintNode> children();
    @Override Collection<? extends BlueprintNode> childValues();

    @Override Optional<? extends BlueprintNode> get(NodePath path);
    @Override Optional<? extends BlueprintNode> get(String... path);

    void visitBlueprints(Consumer<BlueprintNode> visitor);

    @Override BlueprintNode copy();
    @Override BlueprintNode asRoot();

    interface Scoped<
        B extends Scoped<B, N, C, F>,
        N extends TreeNode.Scoped<N, B, C, ? extends FeatureInstance<?, F>, ? extends ItemStack.Scoped<?, B>>,
        C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, F>>,
        F extends FeatureData<F, ?, ? extends FeatureInstance<?, F>>
    > extends BlueprintNode, MapNode.Mutable<B> {
        @Override C component();

        @Override Map<String, F> features();
        @Override Optional<F> feature(String key);

        @Override N asTreeNode(Context context);
    }
}
