package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.nodeview.NodeView;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.incendo.interfaces.core.click.ClickContext;
import org.incendo.interfaces.core.pane.GridPane;
import org.incendo.interfaces.core.transform.Transform;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.element.ItemStackElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class PaperNodeView<T extends GridPane<T, ItemStackElement<T>>> extends NodeView<PaperSlot, PaperNode> implements Transform<T, PlayerViewer> {
    public static final String SLOT_DEFAULT = "default";
    public static final String SLOT_MODIFIABLE = "modifiable";
    public static final String SLOT_REQUIRED = "required";
    public static final String SLOT_COMPATIBLE = "compatible";
    public static final String SLOT_INCOMPATIBLE = "incompatible";
    private final ItemDescriptor defaultSlotItem = new ItemDescriptor(
            Material.BLACK_STAINED_GLASS.getKey(),
            0, 0, false, new ItemFlag[0]
    );

    private final int clickedSlot;
    private final SokolPlugin plugin;
    private final Point2 center;
    private ItemStack nextCursor;

    public PaperNodeView(SokolPlugin plugin, PaperNode root, Options options, int amount, int clickedSlot, Consumer<PaperNode> callback) {
        super(root, options, amount, callback);
        this.plugin = plugin;
        this.clickedSlot = clickedSlot;
        center = plugin.setting(Point2.point2(4, 3), (n, d) -> n.get(Point2.class, d), "node_view", "center");
    }

    public int clickedSlot() { return clickedSlot; }

    private ItemStack slotElement(Locale locale, @Nullable ItemStack cursor, PaperNode parent, Slot slot) {
        String key = SLOT_DEFAULT;
        if (cursor == null) {
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

        ItemStack item = plugin.setting(defaultSlotItem, (n, d) -> n.get(ItemDescriptor.class, d), "node_view", "slot", key)
                .buildStack();
        item.editMeta(meta -> {
            Component slotRender = slot.render(locale, plugin.lc());
            meta.displayName(Components.BLANK.append(plugin.lc().get(locale, "node_view.slot",
                            "name", slotRender)
                    .orElse(slotRender)));
        });
        return item;
    }

    private ItemStack nodeElement(ItemUser user, Locale locale, @Nullable ItemStack cursor, PaperNode parent, Slot slot, PaperNode child) {
        ItemStack item = child.createItem(user).handle();
        item.editMeta(meta -> {
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            //noinspection ConstantConditions
            plugin.lc().lines(locale, "node_view.node",
                            "slot", slot.render(locale, plugin.lc()))
                    .ifPresent(l -> lore.addAll(0, l.stream().map(Components.BLANK::append).collect(Collectors.toList())));
            meta.lore(lore);

            if (cursor != null && plugin.persistence().safeLoad(cursor)
                    .map(cursorNode -> {
                        try {
                            slot.compatibility(parent, cursorNode);
                            return true;
                        } catch (IncompatibilityException e) {
                            return false;
                        }
                    })
                    .orElse(false)) {
                // TODO glint api
                meta.addEnchant(Enchantment.ARROW_INFINITE, 0, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        });
        return item;
    }

    @Override
    public T apply(T pane, InterfaceView<T, PlayerViewer> view) {
        AtomicReference<T> rPane = new AtomicReference<>(pane);
        Player player = view.viewer().player();
        Locale locale = player.locale();
        PlayerUser user = PlayerUser.user(plugin, player);
        InventoryView ivView = player.getOpenInventory();

        ItemStack tCursor = ivView.getCursor();
        ItemStack cursor = tCursor == null || tCursor.getAmount() == 0
                ? nextCursor : tCursor;
        build(locale, cursor == null ? null : plugin.wrap(cursor),
                new NodeView.Renderer<>() {
                    private Point2 correct(Point2 pos) {
                        return new Point2(center.x() + pos.x(), center.y() + pos.y());
                    }

                    @Override
                    public void render(Point2 pos, PaperNode root) {
                        pos = correct(pos);
                        rPane.set(rPane.get().element(ItemStackElement.of(
                                stripSlots(root).createItem(user).handle()
                        ), pos.x(), pos.y()));
                    }

                    private boolean canClick(Slot slot) {
                        return options.modifiable() && (!options.limited() || modifiable(slot));
                    }

                    private void update(InventoryView ivView, ClickContext<?, ?, ?> ctx) {
                        root.initialize(user);
                        nextCursor = ivView.getCursor();
                        if (nextCursor != null && nextCursor.getAmount() <= 0)
                            nextCursor = null;
                        ivView.setCursor(null);
                        ctx.view().update();
                        ivView.setCursor(nextCursor);
                        nextCursor = null;
                        callback.accept(root);
                    }

                    @Override
                    public void render(Point2 pos, PaperNode parent, PaperSlot slot) {
                        pos = correct(pos);
                        rPane.set(rPane.get().element(ItemStackElement.of(
                                slotElement(locale, cursor, parent, slot),
                                canClick(slot) ? ctx -> {
                                    ctx.cancel(true);
                                    if (!ctx.click().leftClick())
                                        return;
                                    InventoryClickEvent event = ctx.cause();
                                    InventoryView ivView = event.getView();
                                    ItemStack ivCursor = ivView.getCursor();
                                    PaperNode nCursor = plugin.persistence().safeLoad(ivCursor).orElse(null);
                                    if (parent.call(new Events.PreModify(parent, slot, user, nCursor)).cancelled())
                                        return;

                                    if (ivCursor == null || ivCursor.getAmount() < amount)
                                        return;
                                    if (nCursor == null)
                                        return;
                                    try {
                                        slot.compatibility(parent, nCursor);
                                        if (
                                                parent.call(new Events.SlotModify(parent, slot, user, null, nCursor)).cancelled()
                                                | nCursor.call(new Events.InsertInto(nCursor, slot, user, parent, null)).cancelled()
                                        ) return;
                                        parent.node(slot.key(), nCursor);
                                    } catch (IncompatibilityException e) {
                                        // TODO event/send msg
                                        return;
                                    }
                                    ivCursor.subtract(amount);
                                    update(ivView, ctx);
                                } : ctx -> {}
                        ), pos.x(), pos.y()));
                    }

                    @Override
                    public void render(Point2 pos, PaperNode parent, PaperSlot slot, PaperNode child) {
                        pos = correct(pos);
                        rPane.set(rPane.get().element(ItemStackElement.of(
                                nodeElement(user, locale, cursor, parent, slot, stripSlots(child)),
                                canClick(slot) ? ctx -> {
                                    ctx.cancel(true);
                                    if (!ctx.click().leftClick())
                                        return;
                                    InventoryClickEvent event = ctx.cause();
                                    InventoryView ivView = event.getView();
                                    ItemStack ivCursor = ivView.getCursor();
                                    PaperNode nCursor = plugin.persistence().safeLoad(ivCursor).orElse(null);
                                    if (parent.call(new Events.PreModify(parent, slot, user, nCursor)).cancelled())
                                        return;

                                    PaperNode nOrphan = child.asRoot();
                                    ItemStack orphan = nOrphan.createItem(user).handle();
                                    if (ivCursor == null || ivCursor.getAmount() == 0) {
                                        if (
                                                parent.call(new Events.SlotModify(parent, slot, user, child, null)).cancelled()
                                                | child.call(new Events.RemoveFrom(child, slot, user, parent, null)).cancelled()
                                        ) return;
                                        parent.removeNode(slot.key());
                                        ivView.setCursor(orphan);
                                    } else {
                                        if (ivCursor.isSimilar(orphan)) {
                                            if (ivCursor.getAmount() + amount > ivCursor.getMaxStackSize())
                                                return;

                                            if (
                                                    parent.call(new Events.SlotModify(parent, slot, user, child, null)).cancelled()
                                                    | child.call(new Events.RemoveFrom(child, slot, user, parent, null)).cancelled()
                                            ) return;
                                            parent.removeNode(slot.key());
                                            ivCursor.add(amount);
                                        } else if (ivCursor.getAmount() == amount) {
                                            try {
                                                //noinspection ConstantConditions
                                                slot.compatibility(parent, nCursor);

                                                if (
                                                        parent.call(new Events.SlotModify(parent, slot, user, child, nCursor)).cancelled()
                                                        | child.call(new Events.RemoveFrom(child, slot, user, parent, nCursor)).cancelled()
                                                        | nCursor.call(new Events.InsertInto(nCursor, slot, user, parent, child)).cancelled()
                                                ) return;
                                                parent.node(slot.key(), nCursor);
                                            } catch (IncompatibilityException e) {
                                                // TODO send message
                                                return;
                                            }
                                            ivView.setCursor(orphan);
                                        } else
                                            return;
                                    }
                                    update(ivView, ctx);
                                } : ctx -> {}
                        ), pos.x(), pos.y()));
                    }
                });
        return rPane.get();
    }

    public static final class Events {
        private Events() {}

        public static class Base implements NodeView.Events.Base<PaperSlot, PaperNode, PaperItem> {
            private final PaperNode node;
            private final PaperSlot slot;
            private final PaperUser user;
            private boolean cancelled;

            public Base(PaperNode node, PaperSlot slot, PaperUser user) {
                this.node = node;
                this.slot = slot;
                this.user = user;
            }

            @Override public PaperNode node() { return node; }
            @Override public PaperSlot slot() { return slot; }
            @Override public PaperUser user() { return user; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class PreModify extends Base implements NodeView.Events.PreModify<PaperSlot, PaperNode, PaperItem> {
            private final @Nullable PaperNode cursor;

            public PreModify(PaperNode node, PaperSlot slot, PaperUser user, @Nullable PaperNode cursor) {
                super(node, slot, user);
                this.cursor = cursor;
            }

            @Override public PaperNode cursor() { return cursor; }
        }

        public static final class SlotModify extends Base implements NodeView.Events.SlotModify<PaperSlot, PaperNode, PaperItem> {
            private final @Nullable PaperNode oldChild;
            private final @Nullable PaperNode newChild;

            public SlotModify(PaperNode node, PaperSlot slot, PaperUser user, @Nullable PaperNode oldChild, @Nullable PaperNode newChild) {
                super(node, slot, user);
                this.oldChild = oldChild;
                this.newChild = newChild;
            }

            @Override public PaperNode oldChild() { return oldChild; }
            @Override public PaperNode newChild() { return newChild; }
        }

        public static final class InsertInto extends Base implements NodeView.Events.InsertInto<PaperSlot, PaperNode, PaperItem> {
            private final PaperNode parent;
            private final @Nullable PaperNode oldChild;

            public InsertInto(PaperNode node, PaperSlot slot, PaperUser user, PaperNode parent, @Nullable PaperNode oldChild) {
                super(node, slot, user);
                this.parent = parent;
                this.oldChild = oldChild;
            }

            @Override public PaperNode parent() { return parent; }
            @Override public PaperNode oldChild() { return oldChild; }
        }

        public static final class RemoveFrom extends Base implements NodeView.Events.RemoveFrom<PaperSlot, PaperNode, PaperItem> {
            private final PaperNode parent;
            private final @Nullable PaperNode newChild;

            public RemoveFrom(PaperNode node, PaperSlot slot, PaperUser user, PaperNode parent, @Nullable PaperNode newChild) {
                super(node, slot, user);
                this.parent = parent;
                this.newChild = newChild;
            }

            @Override public PaperNode parent() { return parent; }
            @Override public PaperNode newChild() { return newChild; }
        }
    }
}
