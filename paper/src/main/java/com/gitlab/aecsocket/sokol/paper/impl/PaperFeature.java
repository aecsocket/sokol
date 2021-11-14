package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.Feature;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;

public interface PaperFeature<F extends PaperFeatureInstance> extends Feature<F, PaperNode> {
    F load(PaperNode node, Type type, ConfigurationNode config) throws SerializationException;
    F load(PaperNode node, PersistentDataContainer pdc) throws IllegalArgumentException;
}
