package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.Feature;
import org.spongepowered.configurate.ConfigurationNode;

import java.lang.reflect.Type;

public interface PaperFeature<F extends PaperFeatureInstance> extends Feature<F, PaperNode> {
    F load(PaperNode node, Type type, ConfigurationNode config);
}
