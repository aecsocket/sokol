package com.github.aecsocket.sokol.core.impl;

import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;

public abstract class AbstractFeatureInstance<
    P extends FeatureProfile<?, D>,
    D extends FeatureData<P, ?, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> implements FeatureInstance<P, D, N> {
    protected N parent;

    @Override public N parent() { return parent; }

    @Override
    public void build(Tree<N> tree, N parent, StatIntermediate stats) {
        this.parent = parent;
    }

    protected void require(String target, TypeToken<?> targetType) throws FeatureValidationException {
        if (!GenericTypeReflector.isSuperType(targetType.getType(), parent.feature(target)
            .orElseThrow(() -> new FeatureValidationException("Could not find feature `" + target + "`"))
            .getClass())) {
            throw new FeatureValidationException("Feature `" + target + "` is not of type " + targetType);
        }
    }
}
