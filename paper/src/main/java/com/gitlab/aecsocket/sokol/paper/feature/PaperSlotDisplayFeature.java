package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.feature.SlotDisplayFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.*;

public final class PaperSlotDisplayFeature extends SlotDisplayFeature<PaperSlotDisplayFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<PaperSlotDisplayFeature.Instance> {
    public static final StatTypes STAT_TYPES = StatTypes.types();
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperSlotDisplayFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_DEFAULT),
            config.node("order").get(Order.class, Order.NATURAL),
            config.node("order_override").getList(String.class, Collections.emptyList()),
            config.node("padding").getString(" ")
    ));

    private final SokolPlugin platform;

    public PaperSlotDisplayFeature(SokolPlugin platform, int listenerPriority, Order order, List<String> orderOverride, String padding) {
        super(listenerPriority, order, orderOverride, padding, platform.font().getWidth(padding) + 1);
        this.platform = platform;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }

    @Override
    protected int width(String text) { return platform.font().getWidth(text); }

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

    public final class Instance extends SlotDisplayFeature<Instance, PaperNode, PaperItem>.Instance implements PaperFeatureInstance {
        public Instance(PaperNode parent) {
            super(parent);
        }

        @Override protected TypeToken<CreateItemEvent<PaperNode, PaperItem>> eventCreateItem() { return new TypeToken<>() {}; }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {}

        @Override
        public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) throws IllegalArgumentException {}

        @Override
        public Instance copy(PaperNode parent) {
            return new Instance(parent);
        }
    }
}
