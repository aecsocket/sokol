package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.StatTypes;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface Feature<
    F extends Feature<F, P>,
    P extends FeatureProfile<P, F, ?>
> extends Keyed {
    StatTypes statTypes();
    RuleTypes ruleTypes();

    P setUp(ConfigurationNode node) throws SerializationException;
}
