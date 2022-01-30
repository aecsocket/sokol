package com.github.aecsocket.sokol.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.MapNode;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.world.ItemCreationException;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface TreeNode extends SokolNode {
    Context context();
    Tree<?> tree();

    SokolComponent component();

    Map<String, ? extends FeatureInstance<?, ?>> features();
    Optional<? extends FeatureInstance<?, ?>> feature(String key);

    BlueprintNode asBlueprintNode();
    ItemStack asItem() throws ItemCreationException;


    @Override @Nullable TreeNode parent();
    @Override TreeNode root();

    @Override Map<String, ? extends TreeNode> children();
    @Override Collection<? extends TreeNode> childValues();

    @Override Optional<? extends TreeNode> get(NodePath path);
    @Override Optional<? extends TreeNode> get(String... path);

    void visitComponents(Consumer<TreeNode> visitor);

    @Override TreeNode copy();
    @Override TreeNode asRoot();

    interface Scoped<
        N extends Scoped<N, B, C, F, S>,
        B extends BlueprintNode.Scoped<B, N, C, ? extends FeatureData<?, ?, F>>,
        C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, ? extends FeatureData<?, ?, F>>>,
        F extends FeatureInstance<F, ? extends FeatureData<?, ?, F>>,
        S extends ItemStack.Scoped<S, B>
    > extends TreeNode, MapNode.Scoped<N> {
        @Override C component();
        @Override Tree<N> tree();

        @Override Map<String, F> features();
        @Override Optional<F> feature(String key);

        @Override B asBlueprintNode();
        @Override S asItem();
    }
}
