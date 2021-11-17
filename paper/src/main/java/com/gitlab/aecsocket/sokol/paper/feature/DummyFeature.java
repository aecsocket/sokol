package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.sokol.core.Pools;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.event.PaperItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.Primitives.*;
import static com.gitlab.aecsocket.sokol.core.stat.Vectors.*;

public class DummyFeature extends AbstractFeature<DummyFeature.Instance, PaperNode> implements PaperFeature<DummyFeature.Instance> {
    public static final String ID = "dummy";
    public static final StatTypes STAT_TYPES = StatTypes.types(
            vector2Stat("durability", Vector2.ZERO),
            decimalStat("damage", 0),
            decimalStat("muzzle_velocity"),
            vector2Stat("spread", Vector2.ZERO),
            vector2Stat("recoil", Vector2.ZERO)
    );
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types()
            .build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new DummyFeature(platform,
            config.node("string_value").getString(""),
            config.node("int_value").getInt(),
            config.node("double_value").getDouble()
    ));

    private final SokolPlugin platform;
    private final String stringValue;
    private final int intValue;
    private final double doubleValue;

    public DummyFeature(SokolPlugin platform, String stringValue, int intValue, double doubleValue) {
        this.platform = platform;
        this.stringValue = stringValue;
        this.intValue = intValue;
        this.doubleValue = doubleValue;
    }

    @Override public String id() { return ID; }
    @Override public SokolPlugin platform() { return platform; }
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
    public Instance load(PaperNode node, PersistentDataContainer pdc) throws IllegalArgumentException {
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

        @Override public DummyFeature type() { return DummyFeature.this; }

        @Override
        public void build(NodeEvent<PaperNode> event, StatIntermediate stats) {
            // TODO test code
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(new TypeToken<PaperItemEvent.Hold>(){}, this::handle);
            });
        }

        private void handle(PaperItemEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (event.user() instanceof Audience audience) {
                audience.sendActionBar(Component.text("holding sync? " + event.sync() + " delta = " + event.context().delta()));
            }
        }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {}

        @Override
        public void save(PersistentDataContainer pdc) throws IllegalArgumentException {}

        @Override
        public Instance copy(PaperNode parent) {
            return new Instance(parent);
        }
    }
}
