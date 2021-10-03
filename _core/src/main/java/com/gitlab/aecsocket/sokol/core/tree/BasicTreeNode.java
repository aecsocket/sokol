package com.gitlab.aecsocket.sokol.core.util;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.feature.Feature;

/**
 * A basic, concrete implementation of a tree node.
 * @param <C> The component type.
 * @param <S> The slot type.
 * @param <B> The base system type.
 * @param <Y> The feature instance type.
 */
public final class BasicTreeNode<C extends Component.Scoped<C, S, B>, S extends Slot, B extends Feature, Y extends Feature.Instance>
        extends AbstractTreeNode<BasicTreeNode<C, S, B, Y>, C, S, B, Y> {
    public BasicTreeNode(C value) {
        super(value);
    }

    @Override protected BasicTreeNode<C, S, B, Y> self() { return this; }

    @Override
    public BasicTreeNode<C, S, B, Y> asRoot() {
        BasicTreeNode<C, S, B, Y> result = new BasicTreeNode<>(value);
        result.children.putAll(children);
        return result;
    }
}
