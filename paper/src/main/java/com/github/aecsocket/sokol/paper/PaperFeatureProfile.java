package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.FeatureProfile;

import org.bukkit.persistence.PersistentDataContainer;

public interface PaperFeatureProfile<
    F extends PaperFeature<?>,
    D extends PaperFeatureData<?, ?>
> extends FeatureProfile<F, D> {
    D load(PersistentDataContainer pdc);
}
