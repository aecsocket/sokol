package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.FeatureInstance;

public interface PaperFeatureInstance<
    P extends PaperFeatureProfile<?, D>,
    D extends PaperFeatureData<P, ?>
> extends FeatureInstance<P, D, PaperTreeNode> {
    PaperFeatureInstance<P, D> copy();
}
