package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.Feature;

public interface PaperFeature<
    P extends PaperFeatureProfile<?, ?>
> extends Feature<P> {}
