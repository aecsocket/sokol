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
import java.util.*;

import com.github.aecsocket.sokol.core.event.CreateItemEvent;
import com.github.aecsocket.sokol.core.feature.StatDisplayFeature;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.StatTypes;

public final class PaperStatDisplayFeature extends StatDisplayFeature<PaperStatDisplayFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<PaperStatDisplayFeature.Instance> {
    public static final StatTypes STAT_TYPES = StatTypes.types();
    public static final Map<String, Class<? extends NodeRule>> RULE_TYPES = NodeRule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new PaperStatDisplayFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_DEFAULT),
            config.node("sections").get(new TypeToken<List<List<Format<?>>>>(){}, Collections.emptyList()),
            config.node("padding").getString(" ")
    ));

    private final SokolPlugin platform;

    public PaperStatDisplayFeature(SokolPlugin platform, int listenerPriority, List<List<Format<?>>> sections, String padding) {
        super(listenerPriority, sections, padding, platform.font().getWidth(padding) + 1);
        this.platform = platform;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }

    @Override
    protected int width(String text) { return platform.font().getWidth(text); }

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

    public final class Instance extends StatDisplayFeature<Instance, PaperNode, PaperItem>.Instance implements PaperFeatureInstance {
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
