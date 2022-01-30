package com.github.aecsocket.sokol.core.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.sokol.core.BlueprintNode;
import com.github.aecsocket.sokol.core.FeatureData;
import com.github.aecsocket.sokol.core.FeatureInstance;
import com.github.aecsocket.sokol.core.FeatureProfile;
import com.github.aecsocket.sokol.core.SokolComponent;
import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.Tree;
import com.github.aecsocket.sokol.core.TreeNode;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.world.ItemStack;

public abstract class AbstractTreeNode<
    N extends AbstractTreeNode<N, B, C, F, S>,
    B extends BlueprintNode.Scoped<B, N, C, ? extends FeatureData<?, ?, F>>,
    C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, ? extends FeatureData<?, ?, F>>>,
    F extends FeatureInstance<F, ? extends FeatureData<?, ?, F>>,
    S extends ItemStack.Scoped<S, B>
> extends MutableAbstractMapNode<N> implements TreeNode.Scoped<N, B, C, F, S> {
    protected final C value;
    protected final Map<String, F> features;
    protected final Context context;
    protected Tree<N> tree;

    public AbstractTreeNode(AbstractTreeNode<N, B, C, F, S> o) {
        super(o);
        value = o.value;
        context = o.context;
        tree = o.tree;

        features = new HashMap<>();
        for (var entry : o.features.entrySet()) {
            features.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public abstract SokolPlatform platform();

    @Override
    public N set(String key, N val) {
        value.slot(key)
            .orElseThrow(() -> new IllegalArgumentException("No slot with ID `" + key + "` on component `" + value.id() + "`"))
            .compatible(val, self());
        return super.set(key, val);
    }

    @Override
    public void visitComponents(Consumer<TreeNode> visitor) {
        visit(visitor::accept);
    }

    protected abstract S createItem();
    protected abstract NodeEvent.CreateItem<N, B, S> createItemEvent(S item);

    @Override
    public S asItem() {
        S item = createItem();
        I18N i18n = platform().i18n();
        Locale locale = context.locale();

        item.name(value.render(i18n, locale));
        tree.events().call(createItemEvent(item));

        return item;
    }
}
