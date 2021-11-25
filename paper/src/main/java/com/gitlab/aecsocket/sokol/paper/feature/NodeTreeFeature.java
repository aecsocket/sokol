package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolInterfaces;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.event.PaperItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.incendo.interfaces.core.view.InterfaceView;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class NodeTreeFeature extends AbstractFeature<NodeTreeFeature.Instance, PaperNode> implements PaperFeature<NodeTreeFeature.Instance> {
    public static final String ID = "node_tree";

    public static final StatTypes STAT_TYPES = StatTypes.types();
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new NodeTreeFeature(platform,
            config.node("listener_priority").getInt(),
            config.node("options").get(SokolInterfaces.NodeTreeOptions.class, SokolInterfaces.NodeTreeOptions.DEFAULT)
    ));

    private final SokolPlugin platform;
    private final int listenerPriority;
    private final SokolInterfaces.NodeTreeOptions options;

    public NodeTreeFeature(SokolPlugin platform, int listenerPriority, SokolInterfaces.NodeTreeOptions options) {
        this.platform = platform;
        this.listenerPriority = listenerPriority;
        this.options = options;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }
    public int listenerPriority() { return listenerPriority; }
    public SokolInterfaces.NodeTreeOptions options() { return options; }

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

        @Override public NodeTreeFeature type() { return NodeTreeFeature.this; }

        @Override
        public void build(NodeEvent<PaperNode> event, StatIntermediate stats) {
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(new TypeToken<PaperItemEvent.SlotClick>(){}, this::onSlotClick, listenerPriority);
            });
        }

        private void onSlotClick(PaperItemEvent.SlotClick event) {
            if (!parent.isRoot())
                return;

            InventoryClickEvent handle = event.handle();
            if (handle.getInventory().getHolder() instanceof InterfaceView<?, ?> ifView) {
                for (var transform : ifView.backing().transformations()) {
                    if (transform.transform() instanceof SokolInterfaces.NodeTreeTransform<?> nodeTreeTf) {
                        if (handle.getSlot() == nodeTreeTf.clickedSlot()) {
                            event.cancel();
                            return;
                        }
                    }
                }
            }

            if (!(event.user() instanceof PlayerUser user))
                return;
            if (event.right() && event.cursor().get().isEmpty()) {
                handle.getView().setCursor(null);
                event.cancel();
                platform.interfaces().openNodeTree(user.entity(), event.node(), event.user(), event.item().name(), options,
                        builder -> builder
                                .clickedSlot(handle.getSlot())
                                .amount(event.item().amount())
                                // TODO update method on event
                                .nodeCallback(node -> {
                                    event.slot().set(node.createItem(user).amount(event.item().amount()));
                                }));
            }
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
