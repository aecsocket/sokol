package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.StatTypes;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface Feature<
    P extends FeatureProfile<?, ?>
> extends Keyed {
    String I18N_KEY = "feature";

    StatTypes statTypes();
    RuleTypes ruleTypes();

    P setUp(ConfigurationNode node) throws SerializationException;

    @Override default String i18nBase() { return I18N_KEY; }
}
