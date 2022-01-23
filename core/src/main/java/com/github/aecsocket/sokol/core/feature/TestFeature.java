package com.github.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.sokol.core.api.*;
import com.gitlab.aecsocket.sokol.core.rule.node.NodeRuleTypes;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class TestFeature implements Feature<TestFeature.Config> {
    public static final String ID = "test";
    public static final StatTypes STAT_TYPES = StatTypes.EMPTY;
    public static final NodeRuleTypes RULE_TYPES = NodeRuleTypes.EMPTY;

    @Override public String id() { return ID; }
    @Override public StatTypes statTypes() { return STAT_TYPES; }
    @Override public NodeRuleTypes ruleTypes() { return RULE_TYPES; }

    @Override
    public Config<?> configure(ConfigurationNode node) throws SerializationException {
        return new Config<>(
                node.node("config_int").getInt()
        );
    }

    public class Config<N extends Node.Scoped<N, ?, ?, ?, ?>> implements FeatureConfig<TestFeature, N, Config.Data> {
        private final int configInt;

        public Config(int configInt) {
            this.configInt = configInt;
        }

        public int configInt() { return configInt; }

        @Override public TestFeature type() { return TestFeature.this; }

        @Override
        public Data<?> setup() {
            return new Data<>();
        }

        @Override
        public Data<?> load(ConfigurationNode node) throws SerializationException {
            return
        }

        public class Data<B extends Blueprint.Scoped<B, N, ?, ?>> implements FeatureData<TestFeature, N, B, ?> {

        }
    }
}
