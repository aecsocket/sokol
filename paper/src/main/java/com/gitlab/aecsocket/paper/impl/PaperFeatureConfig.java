package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.api.FeatureConfig;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;

public interface PaperFeatureConfig extends FeatureConfig<
        PaperFeature, PaperNode, PaperComponent, PaperFeatureData
> {
    PaperFeatureData load(PersistentDataContainer pdc);
    void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx);
}
