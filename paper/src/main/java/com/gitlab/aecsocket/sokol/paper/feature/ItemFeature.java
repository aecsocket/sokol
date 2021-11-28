package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.TreeData;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Primitives;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.gitlab.aecsocket.sokol.core.stat.Primitives.*;

public final class ItemFeature extends AbstractFeature<ItemFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<ItemFeature.Instance> {
    public static final String ID = "item";

    public static final Primitives.OfFlag STAT_UNBREAKABLE = flagStat("unbreakable", false);

    public static final StatTypes STAT_TYPES = StatTypes.types(
            STAT_UNBREAKABLE
    );
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new ItemFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_DEFAULT)
    ));

    private final SokolPlugin platform;
    private final int listenerPriority;

    public ItemFeature(SokolPlugin platform, int listenerPriority) {
        this.platform = platform;
        this.listenerPriority = listenerPriority;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }
    public int listenerPriority() { return listenerPriority; }

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

    public final class Instance extends AbstractInstance<PaperNode> implements PaperFeatureInstance {
        public Instance(PaperNode parent) {
            super(parent);
        }

        @Override public ItemFeature type() { return ItemFeature.this; }

        @Override
        public void build(NodeEvent<PaperNode> event, TreeData.Scoped<PaperNode> tree, StatIntermediate stats) {
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(new TypeToken<CreateItemEvent<PaperNode, PaperItem>>(){}, this::onCreateItem, listenerPriority);
            });
        }

        private void onCreateItem(CreateItemEvent<PaperNode, PaperItem> event) {
            if (!parent.isRoot())
                return;

        }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {}

        @Override
        public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) throws IllegalArgumentException {}

        @Override
        public Instance copy(PaperNode parent) {
            return new Instance(parent);
        }
    }

    @Override public String id() { return ID; }

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) { return Optional.empty(); }
}
