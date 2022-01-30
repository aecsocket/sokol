package com.github.aecsocket.sokol.core;

import com.github.aecsocket.minecommons.core.node.MapNode;

public interface SokolNode extends MapNode {
    SokolComponent value();

    boolean hasFeature(String id);
    FeatureData<?, ?, ?> featureData(String id);
}
