package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.api.FeatureInstance;

public interface PaperFeatureInstance extends FeatureInstance.Scoped<
        PaperFeatureInstance, PaperFeature, PaperNode, PaperFeatureData
> {}
