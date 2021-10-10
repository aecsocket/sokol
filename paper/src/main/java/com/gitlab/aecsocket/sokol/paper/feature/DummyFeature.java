package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Pools;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.gitlab.aecsocket.sokol.core.stat.Primitives.*;

public class DummyFeature extends AbstractFeature<DummyFeature.Instance, PaperNode> implements PaperFeature<DummyFeature.Instance> {
    public static final String ID = "dummy";
    public static final StatTypes STAT_TYPES = StatTypes.types(
            stringStat("string"),
            integerStat("integer", 3),
            decimalStat("decimal", 4.5)
    );
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types()
            .build();

    public static final FeatureType TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, config -> new DummyFeature(
            config.node("string_value").getString(""),
            config.node("int_value").getInt(),
            config.node("double_value").getDouble()
    ));

    private final String stringValue;
    private final int intValue;
    private final double doubleValue;

    public DummyFeature(String stringValue, int intValue, double doubleValue) {
        this.stringValue = stringValue;
        this.intValue = intValue;
        this.doubleValue = doubleValue;
    }

    @Override public String id() { return ID; }
    public String stringValue() { return stringValue; }
    public int intValue() { return intValue; }
    public double doubleValue() { return doubleValue; }

    @Override public void configure(ConfigurationNode config) {}

    @Override
    public Instance create(PaperNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperNode node, Type type, ConfigurationNode config) {
        return new Instance(node);
    }

    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return lc.lines(locale, "feature.dummy.config",
                "string", stringValue,
                "int", intValue+"",
                "double", Pools.decimalFormatter(locale).format(doubleValue));
    }

    public class Instance extends AbstractInstance<PaperNode> implements PaperFeatureInstance {
        public Instance(PaperNode parent) {
            super(parent);
        }

        public Instance(Instance o) {
            super(o);
        }

        @Override public DummyFeature type() { return DummyFeature.this; }

        @Override
        public void build(NodeEvent<PaperNode> event, StatIntermediate stats) {}

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {}

        @Override
        public Instance copy() {
            return new Instance(this);
        }
    }
}
