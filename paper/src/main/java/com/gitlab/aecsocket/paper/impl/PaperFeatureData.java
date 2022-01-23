package com.gitlab.aecsocket.paper.impl;

import com.github.aecsocket.sokol.core.api.FeatureData;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;

public interface PaperFeatureData extends FeatureData<
        PaperFeature, PaperNode, PaperBlueprint, PaperFeatureInstance
> {
    void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx);
}
