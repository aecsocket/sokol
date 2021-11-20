package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.impl.PaperSlot;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.interfaces.core.pane.GridPane;
import org.incendo.interfaces.core.transform.Transform;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.element.ItemStackElement;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SokolInterfaces {
    public static final String TAG_MODIFIABLE = "modifiable";

    public static boolean modifiable(Slot slot) {
        return slot.tagged(TAG_MODIFIABLE);
    }

    public static final String SLOT_DEFAULT = "default";
    public static final String SLOT_MODIFIABLE = "modifiable";
    public static final String SLOT_REQUIRED = "required";
    private static final ItemDescriptor defaultSlotItem = new ItemDescriptor(
            Material.BLACK_STAINED_GLASS_PANE.getKey(),
            0, 0, false, new ItemFlag[0]
    );

    private final SokolPlugin plugin;

    SokolInterfaces(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    @ConfigSerializable
    public record NodeTreeOptions(
            boolean modifiable,
            boolean limited
    ) {
        private static final NodeTreeOptions instance = new NodeTreeOptions();

        public NodeTreeOptions() {
            this(true, true);
        }
    }

    public static class NodeTreeTransform<T extends GridPane<T, ItemStackElement<T>>> implements Transform<T, PlayerViewer> {
        public interface SlotRenderer {
            ItemStack render(Locale locale, PaperSlot slot);
        }

        public interface NodeRenderer {
            ItemStack render(Locale locale, PaperNode node, @Nullable PaperSlot slot);
        }

        private final PaperNode node;
        private final ItemUser user;
        private final Point2 center;
        private final SlotRenderer slotRenderer;
        private final NodeRenderer nodeRenderer;
        private final NodeTreeOptions options;

        public NodeTreeTransform(PaperNode node, ItemUser user, Point2 center, SlotRenderer slotRenderer, NodeRenderer nodeRenderer, NodeTreeOptions options) {
            this.node = node;
            this.user = user;
            this.center = center;
            this.slotRenderer = slotRenderer;
            this.nodeRenderer = nodeRenderer;
            this.options = options;
        }

        public PaperNode node() { return node; }
        public ItemUser user() { return user; }
        public Point2 center() { return center; }
        public SlotRenderer slotRenderer() { return slotRenderer; }
        public NodeRenderer nodeRenderer() { return nodeRenderer; }
        public NodeTreeOptions options() { return options; }

        protected ItemStack slotElement(Locale locale, PaperSlot slot) {
            return slotRenderer.render(locale, slot);
        }

        protected ItemStack nodeElement(Locale locale, PaperNode node, @Nullable PaperSlot slot) {
            return nodeRenderer.render(locale, node, slot);
        }

        protected PaperNode transformNode(PaperNode node) {
            node = node.asRoot();
            for (var key : node.nodes().keySet()) {
                node.unsafeNode(key, null);
            }
            return node;
        }

        protected T apply(T pane, InterfaceView<T, PlayerViewer> view, Locale locale, Point2 position, PaperSlot slot, @Nullable PaperNode node) {
            pane = pane.element(ItemStackElement.of(node == null ? slotElement(locale, slot) : nodeElement(locale, node, slot),
                    ctx -> {
                ctx.viewer().player().sendMessage("loler");
                    }), position.x(), position.y());

            if (node != null) {
                for (var entry : node.value().slots().entrySet()) {
                    var childSlot = entry.getValue();
                    pane = apply(pane, view, locale,
                            new Point2(position.x() + childSlot.offset().x(), position.y() + childSlot.offset().y()),
                            childSlot, node.node(entry.getKey()).orElse(null));
                }
            }
            return pane;
        }

        @Override
        public T apply(T pane, InterfaceView<T, PlayerViewer> view) {
            Locale locale = view.viewer().player().locale();

            pane = pane.element(ItemStackElement.of(nodeElement(locale, node, null)), center.x(), center.y());
            for (var entry : node.value().slots().entrySet()) {
                var slot = entry.getValue();
                pane = apply(pane, view, locale,
                        new Point2(center.x() + slot.offset().x(), center.y() + slot.offset().y()),
                        slot, node.node(entry.getKey()).orElse(null));
            }

            return pane;
        }
    }

    public <T extends GridPane<T, ItemStackElement<T>>> NodeTreeTransform<T> nodeTreeTransform(PaperNode node, ItemUser user, NodeTreeOptions options) {
        return new NodeTreeTransform<>(node, user,
                plugin.setting(new Point2(4, 3), (n, d) -> n.get(Point2.class, d), "node_tree", "center"),
                (locale, slot) -> {
                    String key = SLOT_DEFAULT;
                    if (AbstractNode.required(slot))
                        key = SLOT_REQUIRED;
                    else if (modifiable(slot))
                        key = SLOT_MODIFIABLE;
                    String fKey = key;

                    ItemStack item = plugin.setting(defaultSlotItem, (n, d) -> n.get(ItemDescriptor.class, d), "node_tree", "slot", key)
                            .buildStack();
                    item.editMeta(meta -> {
                        Component slotRender = slot.render(locale, plugin.lc());
                        meta.displayName(Components.BLANK.append(plugin.lc().get(locale, "node_tree.slot",
                                "name", slotRender)
                                .orElse(slotRender)));
                    });
                    return item;
                },
                (locale, rNode, slot) -> {
                    rNode = rNode.asRoot();
                    for (var key : rNode.nodes().keySet()) {
                        rNode.unsafeNode(key, null);
                    }

                    ItemStack item = rNode.createItem(user).handle();
                    if (slot != null) {
                        item.editMeta(meta -> {
                            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                            //noinspection ConstantConditions
                            plugin.lc().lines(locale, "node_tree.node",
                                    "slot", slot.render(locale, plugin.lc()))
                                    .ifPresent(l -> lore.addAll(0, l.stream().map(Components.BLANK::append).collect(Collectors.toList())));
                            meta.lore(lore);
                        });
                    }
                    return item;
                }, options);
    }

    public <T extends GridPane<T, ItemStackElement<T>>> NodeTreeTransform<T> nodeTreeTransform(PaperNode node, ItemUser user) {
        return nodeTreeTransform(node, user,
                plugin.setting(NodeTreeOptions.instance, (n, d) -> n.get(NodeTreeOptions.class, d), "node_tree", "options"));
    }

    public Component nodeTreeTitle(Locale locale, Component nodeName) {
        return plugin.lc().safe(locale, "node_tree.title",
                "node", nodeName);
    }
}
