package com.github.aecsocket.sokol.core;

import java.util.Optional;

import com.github.aecsocket.minecommons.core.node.MapNode;

public interface SokolNode extends MapNode {
    SokolComponent value();

    boolean hasFeature(String key);
    Optional<? extends FeatureData<?, ?, ?, ?>> featureData(String key);
}
