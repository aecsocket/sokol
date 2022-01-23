package old.feature;

import old.FeatureType;
import old.SokolPlugin;
import old.impl.PaperFeature;
import old.impl.PaperFeatureInstance;
import old.impl.PaperNode;
import old.wrapper.PaperItem;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Map;

import com.github.aecsocket.sokol.core.event.CreateItemEvent;
import com.github.aecsocket.sokol.core.feature.ItemDescriptionFeature;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.StatTypes;

public final class PaperItemDescriptionFeature extends ItemDescriptionFeature<PaperItemDescriptionFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<PaperItemDescriptionFeature.Instance> {
    public static final StatTypes STAT_TYPES = StatTypes.types(
            STAT_ITEM_NAME_KEY
    );
    public static final Map<String, Class<? extends NodeRule>> RULE_TYPES = NodeRule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperItemDescriptionFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_DEFAULT)
    ));

    private final SokolPlugin platform;

    public PaperItemDescriptionFeature(SokolPlugin platform, int listenerPriority) {
        super(listenerPriority);
        this.platform = platform;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Instance load(PaperNode node) {
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

    public final class Instance extends ItemDescriptionFeature<Instance, PaperNode, PaperItem>.Instance implements PaperFeatureInstance {
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
