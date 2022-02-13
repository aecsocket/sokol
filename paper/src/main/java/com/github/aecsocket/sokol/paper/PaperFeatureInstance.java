package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.FeatureInstance;

public interface PaperFeatureInstance<
    D extends PaperFeatureData<?, ?>
> extends FeatureInstance<D, PaperTreeNode> {
    PaperFeatureInstance<D> copy();
}
