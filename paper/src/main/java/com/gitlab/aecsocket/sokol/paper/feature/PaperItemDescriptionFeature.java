package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.feature.ItemDescriptionFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Map;

public class PaperItemDescriptionFeature extends ItemDescriptionFeature<PaperItemDescriptionFeature.Instance, PaperNode> implements PaperFeature<PaperItemDescriptionFeature.Instance> {
    public static final StatTypes STAT_TYPES = StatTypes.types(
            STAT_ITEM_NAME_KEY
    );
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperItemDescriptionFeature(platform,
            config.node("listener_priority").getInt()
    ));

    private final SokolPlugin platform;

    public PaperItemDescriptionFeature(SokolPlugin platform, int listenerPriority) {
        super(listenerPriority);
        this.platform = platform;
    }

    @Override public SokolPlugin platform() { return platform; }

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

    public class Instance extends ItemDescriptionFeature<Instance, PaperNode>.Instance implements PaperFeatureInstance {
        public Instance(PaperNode parent) {
            super(parent);
        }

        @Override protected TypeToken<CreateItemEvent<PaperNode>> eventCreateItem() { return new TypeToken<>() {}; }

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
