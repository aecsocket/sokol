package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.impl.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.interfaces.core.pane.GridPane;
import org.incendo.interfaces.core.transform.Transform;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.element.ItemStackElement;
import org.incendo.interfaces.paper.type.ChestInterface;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class SokolInterfaces {
    public static final String TAG_MODIFIABLE = "modifiable";

    public static boolean modifiable(Slot slot) {
        return slot.tagged(TAG_MODIFIABLE);
    }

    public static final String SLOT_DEFAULT = "default";
    public static final String SLOT_MODIFIABLE = "modifiable";
    public static final String SLOT_REQUIRED = "required";
    public static final String SLOT_COMPATIBLE = "compatible";
    public static final String SLOT_INCOMPATIBLE = "incompatible";
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
        public static final NodeTreeOptions DEFAULT = new NodeTreeOptions();

        public NodeTreeOptions() {
            this(true, true);
        }
    }

    public final class NodeTreeTransformBuilder {
        private PaperNode node;
        private ItemUser user;
        private Point2 center;
        private NodeTreeOptions options;

        private int amount;
        private @Nullable Integer clickedSlot;
        private Consumer<PaperNode> nodeCallback;

        public PaperNode node() { return node; }
        public NodeTreeTransformBuilder node(PaperNode node) { this.node = node; return this; }

        public ItemUser user() { return user; }
        public NodeTreeTransformBuilder user(ItemUser user) { this.user = user; return this; }

        public Point2 center() { return center; }
        public NodeTreeTransformBuilder center(Point2 center) { this.center = center; return this; }

        public NodeTreeOptions options() { return options; }
        public NodeTreeTransformBuilder options(NodeTreeOptions options) { this.options = options; return this; }

        public int amount() { return amount; }
        public NodeTreeTransformBuilder amount(int amount) { this.amount = amount; return this; }

        public Integer clickedSlot() { return clickedSlot; }
        public NodeTreeTransformBuilder clickedSlot(Integer clickedSlot) { this.clickedSlot = clickedSlot; return this; }

        public Consumer<PaperNode> nodeCallback() { return nodeCallback; }
        public NodeTreeTransformBuilder nodeCallback(Consumer<PaperNode> nodeCallback) { this.nodeCallback = nodeCallback; return this; }

        private <U> U nonNull(U obj) {
            return Objects.requireNonNull(obj);
        }

        public <T extends GridPane<T, ItemStackElement<T>>> NodeTreeTransform<T> build() {
            return new NodeTreeTransform<>(
                    nonNull(node),
                    nonNull(user),
                    nonNull(center),
                    nonNull(options),
                    amount,
                    clickedSlot,
                    nodeCallback
            );
        }
    }

    public class NodeTreeTransform<T extends GridPane<T, ItemStackElement<T>>> implements Transform<T, PlayerViewer> {
        public interface SlotRenderer {
            ItemStack render(Locale locale, PaperSlot slot);
        }

        public interface NodeRenderer {
            ItemStack render(Locale locale, PaperNode node, @Nullable PaperSlot slot);
        }

        private final PaperNode node;
        private final ItemUser user;
        private final Point2 center;
        private final NodeTreeOptions options;

        private final int amount;
        private final @Nullable Integer clickedSlot;
        private final @Nullable Consumer<PaperNode> nodeCallback;

        private ItemStack nextCursor;

        public NodeTreeTransform(PaperNode node, ItemUser user, Point2 center, NodeTreeOptions options, int amount, @Nullable Integer clickedSlot, Consumer<PaperNode> nodeCallback) {
            this.node = node;
            this.user = user;
            this.center = center;
            this.options = options;
            this.amount = amount;
            this.clickedSlot = clickedSlot;
            this.nodeCallback = nodeCallback;
        }

        public NodeTreeTransform(PaperNode node, ItemUser user, Point2 center, NodeTreeOptions options) {
            this.node = node;
            this.user = user;
            this.center = center;
            this.options = options;
            amount = 1;
            clickedSlot = null;
            nodeCallback = null;
        }

        public PaperNode node() { return node; }
        public ItemUser user() { return user; }
        public Point2 center() { return center; }
        public NodeTreeOptions options() { return options; }

        public int amount() { return amount; }
        public Integer clickedSlot() { return clickedSlot; }
        public @Nullable Consumer<PaperNode> nodeCallback() { return nodeCallback; }

        protected ItemStack slotElement(Locale locale, ItemStack cursor, PaperNode parent, PaperSlot slot) {
            String key = SLOT_DEFAULT;
            if (cursor.getType() == Material.AIR) {
                if (AbstractNode.required(slot))
                    key = SLOT_REQUIRED;
                else if (modifiable(slot))
                    key = SLOT_MODIFIABLE;
            } else {
                key = plugin.persistence().safeLoad(cursor)
                                .map(cursorNode -> {
                                    try {
                                        slot.compatibility(parent, cursorNode);
                                        return true;
                                    } catch (IncompatibilityException e) {
                                        return false;
                                    }
                                }).orElse(false)
                        ? SLOT_COMPATIBLE : SLOT_INCOMPATIBLE;
            }
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
        }

        protected ItemStack nodeElement(Locale locale, ItemStack cursor, PaperNode node) {
            node = node.asRoot();
            for (var key : node.nodes().keySet()) {
                node.unsafeNode(key, null);
            }

            return node.createItem(user).handle();
        }

        protected ItemStack nodeElement(Locale locale, ItemStack cursor, PaperNode parent, PaperNode node, PaperSlot slot) {
            ItemStack item = nodeElement(locale, cursor, node);
            item.editMeta(meta -> {
                List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                //noinspection ConstantConditions
                plugin.lc().lines(locale, "node_tree.node",
                                "slot", slot.render(locale, plugin.lc()))
                        .ifPresent(l -> lore.addAll(0, l.stream().map(Components.BLANK::append).collect(Collectors.toList())));
                meta.lore(lore);

                if (cursor.getType() != Material.AIR && plugin.persistence().safeLoad(cursor)
                        .map(cursorNode -> {
                            try {
                                slot.compatibility(parent, cursorNode);
                                return true;
                            } catch (IncompatibilityException e) {
                                return false;
                            }
                        })
                        .orElse(false)) {
                    // TODO glint
                    meta.addEnchant(Enchantment.ARROW_INFINITE, 0, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            });
            return item;
        }

        protected PaperNode transformNode(PaperNode node) {
            node = node.asRoot();
            for (var key : node.nodes().keySet()) {
                node.unsafeNode(key, null);
            }
            return node;
        }

        protected T apply(T pane, InterfaceView<T, PlayerViewer> view, Locale locale, ItemStack cursor, Point2 position, PaperNode parent, PaperSlot slot, @Nullable PaperNode node) {
            pane = pane.element(ItemStackElement.of(node == null ? slotElement(locale, cursor, parent, slot) : nodeElement(locale, cursor, parent, node, slot),
                    ctx -> {
                        ctx.cancel(true);
                        if (ctx.click().rightClick())
                            // TODO node gets modified here
                            return;
                        if (!options.modifiable || (options.limited && !modifiable(slot)))
                            return;

                        InventoryClickEvent event = ctx.cause();
                        InventoryView invView = event.getView();
                        ItemStack cursorStack = invView.getCursor();
                        PaperNode cursorNode = plugin.persistence().safeLoad(cursorStack).orElse(null);
                        if (cursorStack == null)
                            cursorStack = new ItemStack(Material.AIR);

                        // TODO slot view mod event
                        if (ctx.click().leftClick()) {
                            if (node == null) {
                                if (cursorNode == null)
                                    return;
                                if (cursorStack.getAmount() < amount)
                                    return;
                                try {
                                    parent.node(slot.key(), cursorNode);
                                } catch (IncompatibilityException e) {
                                    // TODO send message here
                                    return;
                                }
                                cursorStack.subtract(amount);
                            } else {
                                PaperNode orphanNode = node.asRoot();
                                ItemStack nodeItem = orphanNode.createItem(user).handle();
                                nodeItem.setAmount(amount);
                                if (cursorStack.getType() == Material.AIR) {
                                    // TODO call evt
                                    parent.removeNode(slot.key());
                                    invView.setCursor(nodeItem);
                                } else {
                                    if (cursorStack.isSimilar(nodeItem)) {
                                        if (cursorStack.getAmount() + amount > cursorStack.getMaxStackSize())
                                            return;
                                        // TODO event
                                        parent.removeNode(slot.key());
                                        cursorStack.add(amount);
                                    } else if (cursorNode != null && cursorStack.getAmount() == amount) {
                                        try {
                                            parent.node(slot.key(), cursorNode);
                                        } catch (IncompatibilityException e) {
                                            // TODO send message
                                            return;
                                        }
                                        invView.setCursor(nodeItem);
                                    } else
                                        return;
                                }
                            }
                        }

                        this.node.initialize(user);
                        nextCursor = invView.getCursor();
                        invView.setCursor(null);
                        ctx.view().update();
                        invView.setCursor(nextCursor);
                        nextCursor = null;
                        if (nodeCallback != null)
                            nodeCallback.accept(this.node);
                    }), position.x(), position.y());

            if (node != null) {
                for (var entry : node.value().slots().entrySet()) {
                    var childSlot = entry.getValue();
                    pane = apply(pane, view, locale, cursor,
                            new Point2(position.x() + childSlot.offset().x(), position.y() + childSlot.offset().y()),
                            node, childSlot, node.node(entry.getKey()).orElse(null));
                }
            }
            return pane;
        }

        @Override
        public T apply(T pane, InterfaceView<T, PlayerViewer> view) {
            Locale locale = view.viewer().player().locale();
            ItemStack cursor = view.viewer().player().getOpenInventory().getCursor();
            if ((cursor == null || cursor.getType() == Material.AIR) && nextCursor != null)
                cursor = nextCursor;
            if (cursor == null)
                cursor = new ItemStack(Material.AIR);

            pane = pane.element(ItemStackElement.of(nodeElement(locale, cursor, node)), center.x(), center.y());
            for (var entry : node.value().slots().entrySet()) {
                var slot = entry.getValue();
                pane = apply(pane, view, locale, cursor,
                        new Point2(center.x() + slot.offset().x(), center.y() + slot.offset().y()),
                        node, slot, node.node(entry.getKey()).orElse(null));
            }

            return pane;
        }
    }

    public NodeTreeTransformBuilder nodeTreeTransformBuilder() {
        return new NodeTreeTransformBuilder();
    }

    public NodeTreeTransformBuilder nodeTreeTransform(PaperNode node, ItemUser user, NodeTreeOptions options) {
        return new NodeTreeTransformBuilder()
                .node(node)
                .user(user)
                .center(plugin.setting(new Point2(4, 3), (n, d) -> n.get(Point2.class, d), "node_tree", "center"))
                .options(options);
    }

    public NodeTreeTransformBuilder nodeTreeTransform(PaperNode node, ItemUser user) {
        return nodeTreeTransform(node, user,
                plugin.setting(NodeTreeOptions.DEFAULT, (n, d) -> n.get(NodeTreeOptions.class, d), "node_tree", "options"));
    }

    public Component nodeTreeTitle(Locale locale, Component nodeName) {
        return plugin.lc().safe(locale, "node_tree.title",
                "node", nodeName);
    }

    public void openNodeTree(Player player, PaperNode node, ItemUser user, Component nodeName, NodeTreeOptions options, Consumer<NodeTreeTransformBuilder> builderFunction) {
        var transform = nodeTreeTransform(node, user, options);
        builderFunction.accept(transform);
        ChestInterface.builder()
                .rows(6)
                .addTransform(transform.build())
                .build()
                .open(PlayerViewer.of(player), nodeTreeTitle(player.locale(), nodeName));
    }

    public void openNodeTree(Player player, PaperNode node, Component nodeName, NodeTreeOptions options, Consumer<NodeTreeTransformBuilder> builderFunction) {
        openNodeTree(player, node, PlayerUser.user(plugin, player), nodeName, options, builderFunction);
    }

    public void openNodeTree(Player player, PaperNode node, Component nodeName, NodeTreeOptions options) {
        openNodeTree(player, node, nodeName, options, b -> {});
    }
}
