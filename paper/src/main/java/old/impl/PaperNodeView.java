package old.impl;

import com.github.aecsocket.sokol.core.Slot;
import com.github.aecsocket.sokol.core.Tree;
import com.github.aecsocket.sokol.core.impl.AbstractNode;
import com.github.aecsocket.sokol.core.node.IncompatibilityException;
import com.github.aecsocket.sokol.core.nodeview.NodeView;
import com.github.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;

import old.ItemDescriptor;
import old.SokolPlugin;
import old.wrapper.PaperItem;
import old.wrapper.user.PaperUser;
import old.wrapper.user.PlayerUser;
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

    public PaperNodeView(SokolPlugin plugin, Tree<PaperNode> tree, Options options, int amount, int clickedSlot, Consumer<Tree<PaperNode>> callback) {
        super(tree, options, amount, callback);
        this.plugin = plugin;
        this.clickedSlot = clickedSlot;
        center = plugin.setting(Point2.point2(4, 3), (n, d) -> n.get(Point2.class, d), "node_view", "center");
    }

    public int clickedSlot() { return clickedSlot; }

    private ItemStack slotElement(ItemUser user, Locale locale, @Nullable ItemStack cursor, PaperNode parent, Slot slot) {
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
                            slot.compatibility(tree, cursorNode.build(user));
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
                            slot.compatibility(tree, cursorNode.build(user));
                            return true;
                        } catch (IncompatibilityException e) {
                            return false;
                        }
                    })
                    .orElse(false)) {
                // TODO glint api
                meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
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
                    public void renderRoot(Point2 pos) {
                        pos = correct(pos);
                        rPane.set(rPane.get().element(ItemStackElement.of(
                                stripSlots(tree.root()).createItem(user).handle()
                        ), pos.x(), pos.y()));
                    }

                    private boolean canClick(Slot slot) {
                        return options.modifiable() && (!options.limited() || modifiable(slot));
                    }

                    private void update(InventoryView ivView, ClickContext<?, ?, ?> ctx) {
                        nextCursor = ivView.getCursor();
                        if (nextCursor != null && nextCursor.getAmount() <= 0)
                            nextCursor = null;
                        ivView.setCursor(null);
                        ctx.view().update();
                        ivView.setCursor(nextCursor);
                        nextCursor = null;
                        callback.accept(tree);
                    }

                    @Override
                    public void renderRoot(Point2 pos, PaperNode parent, PaperSlot slot, @Nullable PaperNode child) {
                        pos = correct(pos);
                        rPane.set(rPane.get().element(ItemStackElement.of(
                                child == null
                                        ? slotElement(user, locale, cursor, parent, slot)
                                        : nodeElement(user, locale, cursor, parent, slot, stripSlots(child)),
                                canClick(slot) ? ctx -> {
                                    ctx.cancel(true);
                                    if (!ctx.click().leftClick())
                                        return;
                                    InventoryClickEvent event = ctx.cause();
                                    InventoryView ivView = event.getView();
                                    ItemStack ivCursor = ivView.getCursor();
                                    PaperNode cursorNode = plugin.persistence().safeLoad(ivCursor).orElse(null);

                                    PaperNode orphanNode = child == null ? null : child.asRoot();
                                    var tree = PaperNodeView.this.tree.root().build(user);
                                    var cursor = cursorNode == null ? null : cursorNode.build(user);
                                    var orphan = orphanNode == null ? null : orphanNode.build(user);
                                    if (
                                            new Events.PreModifyParent(tree, slot, user, child, cursor).call()
                                            | (orphan != null && new Events.PreModifyChild(orphan, slot, user, tree, parent, cursor).call())
                                            | (cursor != null && new Events.PreModifyCursor(cursor, slot, user, tree, parent, node).call())
                                    ) return;

                                    if (orphan == null) {
                                        if (ivCursor == null || ivCursor.getAmount() < amount || cursor == null)
                                            return;
                                        try {
                                            slot.compatibility(tree, cursor);
                                            if (
                                                    new Events.SlotModify(tree, slot, user, null, cursor).call()
                                                    | new Events.InsertInto(cursor, slot, user, parent, null).call()
                                            ) return;
                                            parent.node(slot.key(), cursorNode, root, cursor);
                                            ivCursor.subtract(amount);
                                        } catch (IncompatibilityException e) {
                                            // TODO event/send msg
                                            return;
                                        }
                                    } else {
                                        ItemStack orphanStack = orphanNode.createItem(user).handle();
                                        if (ivCursor == null || ivCursor.getAmount() == 0) {
                                            if (
                                                    root.call(new Events.SlotModify(parent, slot, user, orphanNode, null)).cancelled()
                                                    | orphan.call(new Events.RemoveFrom(orphanNode, slot, user, parent, null)).cancelled()
                                            ) return;
                                            parent.removeNode(slot.key());
                                            ivView.setCursor(orphanStack);
                                        } else {
                                            if (ivCursor.isSimilar(orphanStack)) {
                                                if (ivCursor.getAmount() + amount > ivCursor.getMaxStackSize())
                                                    return;

                                                if (
                                                        root.call(new Events.SlotModify(parent, slot, user, orphanNode, null)).cancelled()
                                                        | orphan.call(new Events.RemoveFrom(orphanNode, slot, user, parent, null)).cancelled()
                                                ) return;
                                                parent.removeNode(slot.key());
                                                ivCursor.add(amount);
                                            } else if (ivCursor.getAmount() == amount) {
                                                try {
                                                    //noinspection ConstantConditions
                                                    slot.compatibility(parent, cursorNode, root, cursor);
                                                    if (
                                                            root.call(new Events.SlotModify(parent, slot, user, orphanNode, cursorNode)).cancelled()
                                                            | orphan.call(new Events.RemoveFrom(orphanNode, slot, user, parent, cursorNode)).cancelled()
                                                            | cursor.call(new Events.InsertInto(cursorNode, slot, user, parent, orphanNode)).cancelled()
                                                    ) return;
                                                    parent.node(slot.key(), cursorNode, root, cursor);
                                                } catch (IncompatibilityException e) {
                                                    // TODO send message
                                                    return;
                                                }
                                                ivView.setCursor(orphanStack);
                                            } else
                                                return;
                                        }
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
            private final Tree<PaperNode> tree;
            private final PaperSlot slot;
            private final PaperUser user;
            private boolean cancelled;

            public Base(Tree<PaperNode> tree, PaperSlot slot, PaperUser user) {
                this.tree = tree;
                this.slot = slot;
                this.user = user;
            }

            @Override public Tree<PaperNode> tree() { return tree; }
            @Override public PaperSlot slot() { return slot; }
            @Override public PaperUser user() { return user; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class PreModifyParent extends Base implements NodeView.Events.PreModifyParent<PaperSlot, PaperNode, PaperItem> {
            private final @Nullable PaperNode childNode;
            private final @Nullable Tree<PaperNode> cursor;

            public PreModifyParent(Tree<PaperNode> tree, PaperSlot slot, PaperUser user, @Nullable PaperNode childNode, @Nullable Tree<PaperNode> cursor) {
                super(tree, slot, user);
                this.childNode = childNode;
                this.cursor = cursor;
            }

            public PaperNode childNode() { return childNode; }
            @Override public @Nullable Tree<PaperNode> cursor() {return cursor;}
        }

        public static final class PreModifyChild extends Base implements NodeView.Events.PreModifyChild<PaperSlot, PaperNode, PaperItem> {
            private final Tree<PaperNode> parent;
            private final PaperNode parentNode;
            private final @Nullable Tree<PaperNode> cursor;

            public PreModifyChild(Tree<PaperNode> tree, PaperSlot slot, PaperUser user, Tree<PaperNode> parent, PaperNode parentNode, @Nullable Tree<PaperNode> cursor) {
                super(tree, slot, user);
                this.parent = parent;
                this.parentNode = parentNode;
                this.cursor = cursor;
            }

            @Override public Tree<PaperNode> parent() { return parent; }
            @Override public PaperNode parentNode() { return parentNode; }
            @Override public Tree<PaperNode> cursor() { return cursor; }
        }

        public static final class PreModifyCursor extends Base implements NodeView.Events.PreModifyCursor<PaperSlot, PaperNode, PaperItem> {
            private final Tree<PaperNode> parent;
            private final PaperNode parentNode;
            private final @Nullable PaperNode child;

            public PreModifyCursor(Tree<PaperNode> tree, PaperSlot slot, PaperUser user, Tree<PaperNode> parent, PaperNode parentNode, @Nullable PaperNode child) {
                super(tree, slot, user);
                this.parent = parent;
                this.parentNode = parentNode;
                this.child = child;
            }

            @Override public Tree<PaperNode> parent() { return parent; }
            @Override public PaperNode parentNode() { return parentNode; }
            @Override public @Nullable PaperNode child() { return child; }
        }

        public static final class SlotModify extends Base implements NodeView.Events.SlotModify<PaperSlot, PaperNode, PaperItem> {
            private final @Nullable PaperNode oldChild;
            private final @Nullable Tree<PaperNode> newChild;

            public SlotModify(Tree<PaperNode> tree, PaperSlot slot, PaperUser user, @Nullable PaperNode oldChild, @Nullable Tree<PaperNode> newChild) {
                super(tree, slot, user);
                this.oldChild = oldChild;
                this.newChild = newChild;
            }

            @Override public PaperNode oldChild() { return oldChild; }
            @Override public Tree<PaperNode> newChild() { return newChild; }
        }

        public static final class InsertInto extends Base implements NodeView.Events.InsertInto<PaperSlot, PaperNode, PaperItem> {
            private final Tree<PaperNode> parent;
            private final PaperNode parentNode;
            private final @Nullable PaperNode oldChild;

            public InsertInto(Tree<PaperNode> tree, PaperSlot slot, PaperUser user, Tree<PaperNode> parent, PaperNode parentNode, @Nullable PaperNode oldChild) {
                super(tree, slot, user);
                this.parent = parent;
                this.parentNode = parentNode;
                this.oldChild = oldChild;
            }

            @Override public Tree<PaperNode> parent() { return parent; }
            @Override public PaperNode parentNode() { return parentNode; }
            @Override public PaperNode oldChild() { return oldChild; }
        }

        public static final class RemoveFrom extends Base implements NodeView.Events.RemoveFrom<PaperSlot, PaperNode, PaperItem> {
            private final Tree<PaperNode> parent;
            private final PaperNode parentNode;
            private final @Nullable Tree<PaperNode> newChild;

            public RemoveFrom(Tree<PaperNode> tree, PaperSlot slot, PaperUser user, Tree<PaperNode> parent, PaperNode parentNode, @Nullable Tree<PaperNode> newChild) {
                super(tree, slot, user);
                this.parent = parent;
                this.parentNode = parentNode;
                this.newChild = newChild;
            }

            @Override public Tree<PaperNode> parent() { return parent; }
            @Override public PaperNode parentNode() { return parentNode; }
            @Override public @Nullable Tree<PaperNode> newChild() { return newChild; }
        }
    }
}
