package com.gitlab.aecsocket.sokol.paper;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.paper.system.PaperItemSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.ItemDescriptor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class SlotViewPane extends Pane {
    public class SlotGuiItem extends GuiItem {
        private final PaperSlot slot;
        private final PaperTreeNode parent;
        private PaperTreeNode node;

        public SlotGuiItem(PaperSlot slot, PaperTreeNode parent, PaperTreeNode node, PaperTreeNode cursor) {
            super(slot == null ? item(node) : createItem(slot, parent, node, cursor));
            this.slot = slot;
            this.parent = parent;
            this.node = node;
            setAction(slot == null ? event -> event.setCancelled(true) : this::event);
        }

        public PaperSlot slot() { return slot; }
        public PaperTreeNode parent() { return parent; }

        public PaperTreeNode node() { return node; }
        public void node(PaperTreeNode node) {
            this.node = node;
            parent.child(slot.key(), node);
        }

        private void event(InventoryClickEvent event) {
            event.setCancelled(true);
            if (event.getClick() == ClickType.RIGHT) {
                return;
            }

            if (!modification || (limited && !slot.fieldModifiable()))
                return;

            InventoryView view = event.getView();
            ItemStack cursor = view.getCursor();
            if (event.getClick() == ClickType.LEFT) {
                PaperTreeNode node = this.node == null ? null : this.node.asRoot();
                PaperTreeNode cursorNode = plugin.persistenceManager().load(cursor);
                PaperItemSystem.Instance itemSystem = node == null ? null : node.systemOf(ItemSystem.ID);

                if (node == null) {
                    if (cursorNode == null || !slot.compatible(parent, cursorNode))
                        return;
                    // Put the cursor into the slot
                    // TODO play sound
                    node(cursorNode);
                    cursor.subtract();
                } else {
                    if (itemSystem == null)
                        return;

                    ItemStack nodeItem = itemSystem.create(locale).handle();
                    if (PaperUtils.empty(cursor)) {
                        // Put the slot item into the cursor
                        node(null);
                        view.setCursor(nodeItem);
                    } else {
                        if (nodeItem.isSimilar(cursor)) {
                            // Add the slot item into the current cursor
                            node(null);
                            cursor.add();
                        } else if (cursorNode != null && slot.compatible(parent, cursorNode) && cursor.getAmount() == 1) {
                            // Swap the cursor and slot item
                            node(cursorNode);
                            view.setCursor(nodeItem);
                        } else
                            return;
                    }
                }

                tree.build();
                updateItems(plugin.persistenceManager().load(view.getCursor()));
                callTreeModify();
            }
        }

        public void updateItem(PaperTreeNode cursor) {
            ItemStack item = createItem(slot, parent, node, cursor);
            if (item == null)
                return;
            ItemStack handle = getItem();
            handle.setType(item.getType());
            handle.setItemMeta(item.getItemMeta());
            applyUUID();
        }
    }

    private final SokolPlugin plugin;
    private Locale locale;
    private PaperTreeNode tree;

    private final Point2 center;
    private final SlotGuiItem[] items;
    private boolean modification;
    private boolean limited;
    private Consumer<PaperTreeNode> treeModifyCallback;

    public SlotViewPane(SokolPlugin plugin, int x, int y, int length, int height, @NotNull Priority priority, Locale locale, PaperTreeNode tree) {
        super(x, y, length, height, priority);
        this.plugin = plugin;
        this.locale = locale;
        this.tree = tree;
        center = new Point2(length / 2, height / 2);
        items = new SlotGuiItem[length * height];
        updateItems(null);
    }

    public SlotViewPane(SokolPlugin plugin, int length, int height, Locale locale, PaperTreeNode tree) {
        super(length, height);
        this.plugin = plugin;
        this.locale = locale;
        this.tree = tree;
        center = new Point2(length / 2, height / 2);
        items = new SlotGuiItem[length * height];
        updateItems(null);
    }

    public SlotViewPane(SokolPlugin plugin, int x, int y, int length, int height, Locale locale, PaperTreeNode tree) {
        super(x, y, length, height);
        this.plugin = plugin;
        this.locale = locale;
        this.tree = tree;
        center = new Point2(length / 2, height / 2);
        items = new SlotGuiItem[length * height];
        updateItems(null);
    }

    public SokolPlugin plugin() { return plugin; }

    public Locale locale() { return locale; }
    public SlotViewPane locale(Locale locale) { this.locale = locale; return this; }

    public PaperTreeNode tree() { return tree; }
    public SlotViewPane tree(PaperTreeNode tree) { this.tree = tree; return this; }

    public boolean modification() { return modification; }
    public SlotViewPane modification(boolean modification) { this.modification = modification; return this; }

    public boolean limited() { return limited; }
    public SlotViewPane limited(boolean limited) { this.limited = limited; return this; }

    public Consumer<PaperTreeNode> treeModifyCallback() { return treeModifyCallback; }
    public SlotViewPane treeModifyCallback(Consumer<PaperTreeNode> treeModifyCallback) { this.treeModifyCallback = treeModifyCallback; return this; }
    public void callTreeModify() {
        if (treeModifyCallback != null)
            treeModifyCallback.accept(tree);
    }

    private ItemStack createItem(PaperSlot slot, PaperTreeNode parent, PaperTreeNode node, PaperTreeNode cursor) {
        if (slot == null)
            return null;
        if (node == null) {
            String itemPath;
            if (cursor == null) {
                if (slot.required())
                    itemPath = "required";
                else if (slot.internal())
                    itemPath = "internal";
                else if (slot.fieldModifiable())
                    itemPath = "field_modifiable";
                else
                    itemPath = "default";
            } else {
                // in/compatible
                itemPath = slot.compatible(parent, cursor)
                        ? (!limited || slot.fieldModifiable()) ? "compatible" : "unusable"
                        : "incompatible";
            }

            return PaperUtils.modify(plugin.setting(plugin.invalidItem(), (n, d) -> n.get(ItemDescriptor.class, d), "slot_view", "gui", "slot", itemPath).createRaw(), meta -> {
                meta.displayName(Components.BLANK.append(slotText(slot)));
            });
        } else {
            return PaperUtils.modify(item(node.asRoot()), meta -> {
                PaperUtils.addLore(meta, Components.BLANK.append(slotText(slot)));
                if (cursor != null && slot.compatible(parent, cursor)) {
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 0, true); // TODO replace with glint API
                }
            });
        }
    }

    private void put(SlotGuiItem item, int x, int y) {
        if (x <= length && y <= height)
            items[(y * length) + x] = item;
    }

    private void updateItems(PaperTreeNode cursor, @NotNull PaperSlot slot, @NotNull PaperTreeNode parent, @Nullable PaperTreeNode node, int cx, int cy) {
        int x = cx + slot.offset().x(), y = cy + slot.offset().y();
        put(new SlotGuiItem(slot, parent, node, cursor), x, y);
        if (node != null) {
            for (var entry : node.slotChildren().entrySet()) {
                updateItems(cursor, entry.getValue().slot(), node, entry.getValue().child(), x, y);
            }
        }
    }

    public void updateItems(PaperTreeNode cursor) {
        Arrays.fill(items, null);

        boolean showInternal = true;
        for (var entry : tree.slotChildren().entrySet()) {
            if (!entry.getValue().slot().internal() && entry.getValue().child() != null) {
                showInternal = false;
                break;
            }
        }

        int x = center.x(), y = center.y();
        put(new SlotGuiItem(null, null, tree, null), x, y);
        for (var entry : tree.slotChildren().entrySet()) {
            PaperSlot slot = entry.getValue().slot();
            if (slot.internal() && !showInternal)
                continue;
            updateItems(cursor, slot, tree, entry.getValue().child(), x, y);
        }
    }

    private ItemStack item(PaperTreeNode node) {
        PaperItemSystem.Instance itemSystem = node.systemOf(ItemSystem.ID);
        if (itemSystem == null) {
            return PaperUtils.modify(plugin.invalidItem().createRaw(), meta -> {
                meta.displayName(Components.BLANK.append(node.value().name(locale)));
            });
        } else {
            return itemSystem.create(locale).handle();
        }
    }

    private Component slotText(PaperSlot slot) {
        return plugin.localize(locale, "slot.meta.name",
                "name", slot.name(locale),
                "required", plugin.localize(locale, "slot.meta." + (slot.required() ? "" : "not_") + "required"),
                "internal", plugin.localize(locale, "slot.meta." + (slot.internal() ? "" : "not_") + "internal"),
                "field_modifiable", plugin.localize(locale, "slot.meta." + (slot.fieldModifiable() ? "" : "not_") + "field_modifiable"));
    }

    private Collection<GuiItem> toItems() {
        Collection<GuiItem> result = new ArrayList<>();
        for (SlotGuiItem item : items) {
            if (item != null)
                result.add(item);
        }
        return result;
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

    @Override
    public boolean click(@NotNull Gui gui, @NotNull InventoryComponent inv, @NotNull InventoryClickEvent event, int slot, int offX, int offY, int maxLength, int maxHeight) {
        callOnClick(event);

        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null)
            return false;
        GuiItem item = findMatchingItem(toItems(), itemStack);
        if (item == null)
            return false;
        item.callAction(event);
        return true;
    }

    public void handleGlobalClick(Gui gui, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        PaperTreeNode clickedNode = plugin.persistenceManager().load(clicked);
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            if (PaperUtils.empty(clicked))
                event.setCancelled(true);
        } else {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }
            if (!plugin.persistenceManager().isTree(event.getCursor()) && clickedNode == null)
                return;

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                updateItems(plugin.persistenceManager().load(event.getCursor()));
                gui.update();
            });
        }
    }

    @Override public @NotNull Collection<GuiItem> getItems() { return Arrays.asList(items); }
    @Override public @NotNull Collection<Pane> getPanes() { return new ArrayList<>(); }
    @Override public void clear() {}
}
