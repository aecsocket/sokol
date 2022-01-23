package com.github.aecsocket.sokol.core.api;

import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.rule.node.NodeRuleTypes;
import com.github.aecsocket.sokol.core.stat.StatTypes;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface Feature<
        C extends FeatureConfig<?, ?, ?>
> extends Keyed {
    StatTypes statTypes();
    NodeRuleTypes ruleTypes();

    C configure(ConfigurationNode node) throws SerializationException;
}
