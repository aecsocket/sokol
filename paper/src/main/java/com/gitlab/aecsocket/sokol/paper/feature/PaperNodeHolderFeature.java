package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.minecommons.core.Quantifier;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PaperNodeHolderFeature extends NodeHolderFeature<PaperNodeHolderFeature.Instance, PaperNode> implements PaperFeature<PaperNodeHolderFeature.Instance> {
    public static final StatTypes STAT_TYPES = StatTypes.types();
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperNodeHolderFeature(platform,
            config.node("listener_priority").getInt(),
            Rule.Constant.TRUE,
            config.node("header_position").get(Position.class, Position.TOP),
            config.node("capacity").get(Integer.class),
            config.node("show_full_as_durability").getBoolean(true)
    ));

    private final SokolPlugin platform;

    public PaperNodeHolderFeature(SokolPlugin platform, int listenerPriority, Rule rule, Position headerPosition, @Nullable Integer capacity, boolean showFullAsDurability) {
        super(listenerPriority, rule, headerPosition, capacity, showFullAsDurability);
        this.platform = platform;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {
        rule = config.node("rule").get(Rule.class);
    }

    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(PaperNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperNode node, Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node, config.node("nodes").getList(new TypeToken<Quantifier<PaperNode>>(){}, Collections.emptyList()));
    }

    @Override
    public Instance load(PaperNode node, PersistentDataContainer pdc) throws IllegalArgumentException {
        PersistentDataContainer[] fNodes = pdc.get(platform.key("nodes"), PersistentDataType.TAG_CONTAINER_ARRAY);
        if (fNodes == null)
            return new Instance(node);
        List<Quantifier<PaperNode>> nodes = new ArrayList<>();
        for (var fQt : fNodes) {
            //noinspection ConstantConditions
            platform.persistence().safeLoad(fQt.get(platform.key("value"), PersistentDataType.TAG_CONTAINER))
                            .ifPresent(value -> nodes.add(new Quantifier<>(
                                    value,
                                    fQt.getOrDefault(platform.key("amount"), PersistentDataType.INTEGER, 0)
                            )));
        }
        return new Instance(node, nodes);
    }

    public final class Instance extends NodeHolderFeature<Instance, PaperNode>.Instance implements PaperFeatureInstance {
        public Instance(PaperNode parent, List<Quantifier<PaperNode>> nodes) {
            super(parent, nodes);
        }

        public Instance(PaperNode parent) {
            super(parent);
        }

        @Override protected TypeToken<CreateItemEvent<PaperNode>> eventCreateItem() { return new TypeToken<>() {}; }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {
            node.node("nodes").set(nodes);
        }

        @Override
        public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer[] fNodes = new PersistentDataContainer[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                var qt = nodes.get(i);
                PersistentDataContainer fQt = ctx.newPersistentDataContainer();
                fQt.set(platform.key("amount"), PersistentDataType.INTEGER, qt.amount());
                fQt.set(platform.key("value"), PersistentDataType.TAG_CONTAINER, platform.persistence().save(ctx.newPersistentDataContainer(), qt.object()));
                fNodes[i] = fQt;
            }
            pdc.set(platform.key("nodes"), PersistentDataType.TAG_CONTAINER_ARRAY, fNodes);
        }

        @Override
        public Instance copy(PaperNode parent) {
            return new Instance(parent);
        }
    }
}
