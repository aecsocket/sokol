package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.minecommons.core.Components;
import com.github.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.github.aecsocket.sokol.core.IncompatibleException;
import com.github.aecsocket.sokol.core.nodeview.NodeView;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class PaperNodeView extends Pane {
    public static final String
        NODE_VIEW_TITLE = "node_view.title",
        NODE_VIEW_NODE_LORE = "node_view.node_lore",
        NODE_VIEW_SLOT = "node_view.slot",
        SLOT_DEFAULT = "default",
        SLOT_REQUIRED = "required",
        SLOT_MODIFIABLE = "modifiable",
        SLOT_COMPATIBLE = "compatible",
        SLOT_INCOMPATIBLE = "incompatible";
    private static final ItemDescriptor SLOT_ITEM_DEFAULT = new ItemDescriptor(Material.BLACK_STAINED_GLASS_PANE.getKey(), 0, 0, false, new ItemFlag[0]);

    private final SokolPlugin plugin;
    private final NodeView<PaperTreeNode, PaperNodeSlot> backing;
    private final int clickedSlot;
    private @Nullable ItemStack cursor;

    private final GuiItem[] items;

    public PaperNodeView(int x, int y, int length, int height, @NotNull Priority priority, SokolPlugin plugin, NodeView<PaperTreeNode, PaperNodeSlot> backing, int clickedSlot) {
        super(x, y, length, height, priority);
        this.plugin = plugin;
        this.backing = backing;
        this.clickedSlot = clickedSlot;
        buildItems();
    }

    public PaperNodeView(int length, int height, SokolPlugin plugin, NodeView<PaperTreeNode, PaperNodeSlot> backing, int clickedSlot) {
        super(length, height);
        this.plugin = plugin;
        this.backing = backing;
        this.clickedSlot = clickedSlot;
        buildItems();
    }

    public PaperNodeView(int x, int y, int length, int height, SokolPlugin plugin, NodeView<PaperTreeNode, PaperNodeSlot> backing, int clickedSlot) {
        super(x, y, length, height);
        this.plugin = plugin;
        this.backing = backing;
        this.clickedSlot = clickedSlot;
        buildItems();
    }

    {
        items = new GuiItem[length * height];
    }

    public NodeView<PaperTreeNode, PaperNodeSlot> backing() { return backing; }
    public int clickedSlot() { return clickedSlot; }
    public GuiItem[] items() { return items; }

    void cursor(@Nullable ItemStack cursor) {
        this.cursor = cursor;
        buildItems();
    }

    private ItemStack buildNodeItem(PaperTreeNode node) {
        node = node.asRoot();
        node.clearChildren();
        return node.build().asStack().handle();
    }

    private ItemStack buildSlotItem(Locale locale, PaperTreeNode parent, PaperNodeSlot slot) {
        String key = SLOT_DEFAULT;
        if (cursor == null || cursor.getAmount() == 0) {
            if (slot.required())
                key = SLOT_REQUIRED;
            else if (slot.modifiable())
                key = SLOT_MODIFIABLE;
        } else {
            key = plugin.persistence().load(cursor)
                .map(cursorNode -> {
                    try {
                        slot.compatible(cursorNode, parent);
                        return true;
                    } catch (IncompatibleException e) {
                        return false;
                    }
                }).orElse(false)
                ? SLOT_COMPATIBLE : SLOT_INCOMPATIBLE;
        }

        ItemStack item = plugin.setting(SLOT_ITEM_DEFAULT, (n, d) -> n.get(ItemDescriptor.class, d), "node_view", "slot", key)
            .stack();
        item.editMeta(meta -> meta.displayName(Components.BLANK.append(plugin.i18n().line(locale, NODE_VIEW_SLOT,
            c -> c.of("key", () -> Component.text(slot.key())),
            c -> c.of("slot", () -> c.rd(slot))))));
        return item;
    }

    private GuiItem buildRoot() {
        return new GuiItem(buildNodeItem(backing.root()), event -> event.setCancelled(true));
    }

    private void set(int x, int y, GuiItem item) {
        if (x <= length && y <= height)
            items[(y * length) + x] = item;
    }

    public void buildItems() {
        Arrays.fill(items, null);
        Point2 pos = plugin.setting(Point2.point2(4, 3), (n, d) -> n.get(Point2.class, d), "node_view", "center");
        set(pos.x(), pos.y(), buildRoot());
        build(backing.root(), pos);
    }

    private void update() {
        backing.root().build();
        backing.callback().accept(backing.root());
        buildItems();
    }

    @Override
    public void display(@NotNull InventoryComponent inv, int offX, int offY, int maxLength, int maxHeight) {
        for (int i = 0; i < items.length; i++) {
            GuiItem item = items[i];
            if (item == null || !item.isVisible())
                continue;
            inv.setItem(items[i], i % length, i / length);
        }
    }

    private void build(PaperTreeNode node, Point2 origin) {
        Locale locale = node.context().locale();
        for (var entry : node.value().slots().entrySet()) {
            String key = entry.getKey();
            PaperNodeSlot slot = entry.getValue();
            Point2 pos = Point2.point2(origin.x() + slot.offset().x(), origin.y() + slot.offset().y());

            PaperTreeNode child = node.get(key).orElse(null);
            ItemStack item;
            if (child == null) {
                item = buildSlotItem(locale, node, slot);
            } else {
                item = buildNodeItem(child);
                item.editMeta(meta -> {
                    List<Component> lore = meta.lore();
                    if (lore == null)
                        lore = new ArrayList<>();
                    lore.addAll(plugin.i18n().modLines(locale, NODE_VIEW_NODE_LORE,
                        Components.BLANK::append,
                        c -> c.of("key", () -> Component.text(key)),
                        c -> c.of("slot", () -> c.rd(slot))));
                    meta.lore(lore);

                    if (cursor != null && plugin.persistence().load(cursor)
                        .map(cursorNode -> {
                            try {
                                slot.compatible(cursorNode, node);
                                return true;
                            } catch (IncompatibleException e) {
                                return false;
                            }
                        }).orElse(false)
                    ) {
                        // TODO glint API
                        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                });
                build(child, pos);
            }

            set(pos.x(), pos.y(), new GuiItem(item,
                backing.options().modifiable()
                && (!backing.options().limited() || slot.modifiable()) ? event -> {
                if (!event.isLeftClick())
                    return;
                if (!(event.getWhoClicked() instanceof Player player))
                    return;

                PaperTreeNode orphan = child == null ? null : child.asRoot().build();
                ItemStack cursorStack = event.getCursor();
                PaperTreeNode cursor = plugin.persistence().load(cursorStack)
                    .map(bp -> bp.asTreeNode(PaperContext.context(PaperItemUser.user(plugin, player))))
                    .orElse(null);

                // pre modify evt

                int amount = backing.amount();
                if (orphan == null) {
                    // try to place a node into an empty slot
                    if (cursorStack == null || cursorStack.getAmount() < amount || cursor == null)
                        return;
                    try {
                        // evt
                        node.set(key, cursor);
                        cursorStack.subtract(amount);
                    } catch (IncompatibleException e) {
                        // TODO send message
                        return;
                    }
                } else {
                    ItemStack orphanStack = orphan.asStack().handle();
                    if (cursorStack == null || cursorStack.getAmount() == 0) {
                        // evt
                        node.removeChild(key);
                        event.getView().setCursor(orphanStack);
                    } else {
                        if (cursorStack.isSimilar(orphanStack)) {
                            if (cursorStack.getAmount() + amount > cursorStack.getMaxStackSize())
                                return;
                            // evt
                            node.removeChild(key);
                            cursorStack.add(amount);
                        } else if (cursorStack.getAmount() == amount) {
                            try {
                                // evt
                                //noinspection ConstantConditions
                                node.set(key, cursor);
                            } catch (IncompatibleException e) {
                                // message
                                return;
                            }
                            event.getView().setCursor(orphanStack);
                        } else
                            return;
                    }
                }

                update();
            } : null));
        }
    }


    @Override
    public boolean click(@NotNull Gui gui, @NotNull InventoryComponent inv, @NotNull InventoryClickEvent event, int slot, int offX, int offY, int maxLength, int maxHeight) {
        ItemStack stack = event.getCurrentItem();
        if (stack == null)
            return false;
        GuiItem item = findMatchingItem(getItems(), stack);
        if (item == null)
            return false;
        item.callAction(event);
        return true;
    }

    @Override
    public @NotNull Collection<GuiItem> getItems() {
        List<GuiItem> items = new ArrayList<>();
        for (var item : this.items) {
            if (item != null)
                items.add(item);
        }
        return items;
    }

    @Override public @NotNull Collection<Pane> getPanes() { return Collections.emptyList(); }

    @Override
    public void clear() {
        Arrays.fill(items, null);
    }
}
