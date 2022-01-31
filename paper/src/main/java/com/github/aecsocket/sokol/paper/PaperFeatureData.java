package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.FeatureData;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;

public interface PaperFeatureData extends FeatureData<
    PaperFeatureData, PaperFeatureProfile, PaperFeatureInstance, PaperTreeNode
> {
    void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx);
}
