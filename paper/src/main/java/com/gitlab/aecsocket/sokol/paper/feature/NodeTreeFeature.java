package com.gitlab.aecsocket.sokol.paper.feature;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
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
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
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

public final class NodeTreeFeature extends AbstractFeature<NodeTreeFeature.Instance, PaperNode, PaperItem>
        implements PaperFeature<NodeTreeFeature.Instance> {
    public static final String ID = "node_tree";

    public static final StatTypes STAT_TYPES = StatTypes.types();
    public static final Map<String, Class<? extends Rule>> RULE_TYPES = Rule.types().build();

    public static final FeatureType.Keyed TYPE = FeatureType.of(ID, STAT_TYPES, RULE_TYPES, (platform, config) -> new NodeTreeFeature(platform,
            config.node("listener_priority").getInt(PRIORITY_HIGH),
            config.node("options").get(SokolInterfaces.NodeTreeOptions.class, SokolInterfaces.NodeTreeOptions.DEFAULT),
            config.node("combine").getBoolean(true),
            config.node("combine_limited").getBoolean(true)
    ));

    private final SokolPlugin platform;
    private final int listenerPriority;
    private final SokolInterfaces.NodeTreeOptions options;
    private final boolean combine;
    private final boolean combineLimited;

    public NodeTreeFeature(SokolPlugin platform, int listenerPriority, SokolInterfaces.NodeTreeOptions options, boolean combine, boolean combineLimited) {
        this.platform = platform;
        this.listenerPriority = listenerPriority;
        this.options = options;
        this.combine = combine;
        this.combineLimited = combineLimited;
    }

    @Override
    public void configure(ConfigurationNode config) throws SerializationException {}

    @Override public SokolPlugin platform() { return platform; }
    public int listenerPriority() { return listenerPriority; }
    public SokolInterfaces.NodeTreeOptions options() { return options; }
    public boolean combine() { return combine; }
    public boolean combineLimited() { return combineLimited; }

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
                events.register(new TypeToken<PaperItemEvent.SlotDrag>(){}, this::onSlotDrag, listenerPriority);
            });
        }

        private boolean shouldCancel(InventoryEvent event, int slot) {
            if (event.getInventory().getHolder() instanceof InterfaceView<?, ?> ifView) {
                for (var transform : ifView.backing().transformations()) {
                    if (transform.transform() instanceof SokolInterfaces.NodeTreeTransform<?> nodeTreeTf) {
                        if (slot == nodeTreeTf.clickedSlot()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void onSlotClick(PaperItemEvent.SlotClick event) {
            if (!parent.isRoot())
                return;
            if (event.cancelled())
                return;


            // cancel interactions on this item if in a node tree view
            InventoryClickEvent handle = event.handle();
            if (shouldCancel(handle, handle.getSlot())) {
                event.cancel();
                return;
            }

            if (!(event.user() instanceof PlayerUser user))
                return;

            PaperNode node = event.node();
            ItemStack cursorStack = event.cursor().bukkitGet();
            if (cursorStack == null || cursorStack.getAmount() == 0) {
                if (event.right()) {
                    handle.getView().setCursor(null);
                    event.cancel();
                    platform.interfaces().openNodeTree(user.entity(), node, event.user(), event.item().name(), options,
                            builder -> builder
                                    .clickedSlot(handle.getSlot())
                                    .amount(event.item().amount())
                                    // TODO update method on event
                                    .nodeCallback(newNode -> {
                                        event.slot().set(newNode.createItem(user).amount(event.item().amount()));
                                    }));
                }
            } else if (event.left() && combine) {
                ItemStack clickedStack = event.item().handle();
                platform.persistence().safeLoad(cursorStack).ifPresent(cursorNode -> {
                    if (!combine(node.root(), cursorNode))
                        return;
                    node.root().initialize(user);
                    event.cancel();
                    // TODO call events
                    int amtCursor = cursorStack.getAmount();
                    int amtClicked = clickedStack.getAmount();
                    if (amtCursor >= amtClicked) {
                        // UPDATE
                        event.slot().set(node.createItem(user).amount(amtClicked));
                        cursorStack.subtract(amtClicked);
                    } else {
                        event.cursor().set(node.createItem(user).amount(amtCursor));
                        clickedStack.subtract(amtCursor);
                    }
                });
            }
        }

        private boolean combine(PaperNode parent, PaperNode append) {
            for (var entry : parent.value().slots().entrySet()) {
                if (combineLimited && !SokolInterfaces.modifiable(entry.getValue()))
                    continue;
                try {
                    parent.node(entry.getKey(), append);
                } catch (IncompatibilityException e) {
                    continue;
                }
                return true;
            }
            for (var child : parent.nodes().values())  {
                if (combine(child, append))
                    return true;
            }
            return false;
        }

        private void onSlotDrag(PaperItemEvent.SlotDrag event) {
            if (!parent.isRoot())
                return;

            InventoryDragEvent handle = event.handle();
            InventoryView view = handle.getView();
            // cancel interactions on this item if in a node tree view
            int rawSlot = event.rawSlot();
            if (view.getInventory(rawSlot) == view.getBottomInventory() && shouldCancel(handle, view.convertSlot(rawSlot))) {
                event.cancel();
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
