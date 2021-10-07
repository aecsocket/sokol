package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.FeatureInstance;

public interface PaperFeatureInstance extends FeatureInstance<PaperNode> {
    @Override PaperFeatureInstance copy();
}
