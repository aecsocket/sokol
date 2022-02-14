package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.minecommons.core.Components;
import com.github.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.github.aecsocket.sokol.core.IncompatibleException;
import com.github.aecsocket.sokol.core.nodeview.NodeView;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
    }

    public PaperNodeView(int length, int height, SokolPlugin plugin, NodeView<PaperTreeNode, PaperNodeSlot> backing, int clickedSlot) {
        super(length, height);
        this.plugin = plugin;
        this.backing = backing;
        this.clickedSlot = clickedSlot;
    }

    public PaperNodeView(int x, int y, int length, int height, SokolPlugin plugin, NodeView<PaperTreeNode, PaperNodeSlot> backing, int clickedSlot) {
        super(x, y, length, height);
        this.plugin = plugin;
        this.backing = backing;
        this.clickedSlot = clickedSlot;
    }

    {
        items = new GuiItem[length * height];
    }

    public NodeView<PaperTreeNode, PaperNodeSlot> backing() { return backing; }
    public int clickedSlot() { return clickedSlot; }
    public GuiItem[] items() { return items; }

    void cursor(@Nullable ItemStack cursor) { this.cursor = cursor; }

    private void callback() {
        backing.callback().accept(backing.root());
    }

    private void set(InventoryComponent inv, int offX, int offY, int maxLength, int maxHeight, int x, int y, GuiItem item) {
        if (x + offX > Math.min(length, maxLength) || y + offY > Math.min(height, maxHeight))
            return;
        inv.setItem(item, x, y);
    }

    private ItemStack buildNodeItem(PaperTreeNode node) {
        node = node.asRoot();
        node.clearChildren();
        return node.build().asItem().handle();
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

    @Override
    public void display(@NotNull InventoryComponent inv, int offX, int offY, int maxLength, int maxHeight) {
        Point2 pos = backing.options().center();
        set(inv, offX, offY, maxLength, maxHeight, pos.x(), pos.y(), buildRoot());
        build(inv, offX, offY, maxLength, maxHeight, backing.root(), pos);
    }

    private void build(InventoryComponent inv, int offX, int offY, int maxLength, int maxHeight, PaperTreeNode node, Point2 origin) {
        Locale locale = node.context().locale();
        for (var entry : node.value().slots().entrySet()) {
            String key = entry.getKey();
            PaperNodeSlot slot = entry.getValue();
            Point2 pos = Point2.point2(origin.x() + slot.offset().x(), origin.y() + slot.offset().y());

            node.get(key).ifPresentOrElse(child -> {
                ItemStack item = buildNodeItem(child);
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
                set(inv, offX, offY, maxLength, maxHeight, pos.x(), pos.y(), new GuiItem(item, event -> {
                }));
                build(inv, offX, offY, maxLength, maxHeight, child, pos);
            }, () -> {
                ItemStack item = buildSlotItem(locale, node, slot);
                set(inv, offX, offY, maxLength, maxHeight, pos.x(), pos.y(), new GuiItem(item, event -> {
                }));
            });
        }
    }


    @Override
    public boolean click(@NotNull Gui gui, @NotNull InventoryComponent inv, @NotNull InventoryClickEvent event, int slot, int offX, int offY, int maxLength, int maxHeight) {
        ItemStack item = event.getCurrentItem();
        if (item == null)
            return false;
        return false;
    }

    @Override public @NotNull Collection<GuiItem> getItems() { return Arrays.asList(items); }
    @Override public @NotNull Collection<Pane> getPanes() { return Collections.emptyList(); }

    @Override
    public void clear() {
        Arrays.fill(items, null);
    }
}
