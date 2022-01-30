package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.FeatureProfile;

import org.bukkit.persistence.PersistentDataContainer;

public interface PaperFeatureProfile extends FeatureProfile<
    PaperFeatureProfile, PaperFeature, PaperFeatureData
> {
    PaperFeatureData load(PersistentDataContainer pdc);
}
