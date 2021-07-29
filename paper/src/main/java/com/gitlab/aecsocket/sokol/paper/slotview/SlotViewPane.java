package com.gitlab.aecsocket.sokol.paper.slotview;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.paper.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.inbuilt.SlotsSystem;
import com.gitlab.aecsocket.sokol.paper.system.inbuilt.PaperItemSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class SlotViewPane extends Pane {
    public class Item extends GuiItem {
        private final PaperSlot slot;
        private final PaperTreeNode parent;
        private PaperTreeNode child;

        public Item(@Nullable PaperSlot slot, @Nullable PaperTreeNode parent, @Nullable PaperTreeNode child, @Nullable PaperTreeNode cursor) {
            super(slot == null ? item(child) : createItem(slot, parent, child, cursor));
            this.slot = slot;
            this.parent = parent;
            this.child = child;
            setAction(slot == null ? event -> event.setCancelled(true) : this::event);
        }

        public SlotViewPane pane() { return SlotViewPane.this; }

        public PaperSlot slot() { return slot; }
        public PaperTreeNode parent() { return parent; }

        public PaperTreeNode child() { return child; }
        public void child(@Nullable PaperTreeNode child) {
            this.child = child;
            parent.child(slot.key(), child);
        }

        private void event(InventoryClickEvent event) {
            event.setCancelled(true);
            if (event.getClick() == ClickType.RIGHT) {
                return;
            }

            if (!modification || (limited && !slot.fieldModifiable()))
                return;

            InventoryView view = event.getView();
            ItemStack cursor = PaperUtils.normalize(view.getCursor());
            PaperTreeNode cursorNode = plugin.persistenceManager().load(cursor).orElse(null);
            if (!new SlotViewModifyEvent(event, this, cursorNode).callEvent())
                return;

            /*
            _, _ -> []
            slotNode, _ -> [
                parent: SlotModify[oldNode=slotNode, newNode=null],
                slotNode: RemoveFrom[slot, parent, replacement=null]
            ]
            _, cursorNode -> [
                parent: SlotModify[oldNode=null, newNode=cursorNode],
                cursorNode: InsertInto[slot, parent, replacing=null]
            ]
            slotNode, cursorNode -> [
                parent: SlotModify[oldNode=slotNode, newNode=cursorNode],
                slotNode: RemoveFrom[slot, parent, replacement=cursorNode]
                cursorNode: InsertInto[slot, parent, replacing=slotNode]
            ]
             */
            if (event.getClick() == ClickType.LEFT) {
                if (child == null) {
                    if (cursorNode == null || !slot.compatible(parent, cursorNode))
                        return;
                    // cursorNode -> [slot]
                    // _, cursorNode
                    SlotsSystem.Events.SlotModify slotModify = new SlotsSystem.Events.SlotModify(event, parent, slot, null, cursorNode);
                    SlotsSystem.Events.InsertInto insertInto = new SlotsSystem.Events.InsertInto(event, cursorNode, parent, slot, null);
                    if (slotModify.call() | insertInto.call())
                        return;
                    child(slotModify.newChild().orElse(null));
                    cursor.subtract();
                } else {
                    PaperTreeNode orphanChild = child.asRoot();
                    PaperItemSystem.Instance itemSystem = orphanChild.system(PaperItemSystem.KEY).orElse(null);
                    if (itemSystem == null)
                        return;

                    ItemStack nodeItem = itemSystem.create(locale).handle();
                    if (PaperUtils.empty(cursor)) {
                        // child -> [cursor]
                        // slotNode, _
                        SlotsSystem.Events.SlotModify slotModify = new SlotsSystem.Events.SlotModify(event, parent, slot, child, null);
                        SlotsSystem.Events.RemoveFrom removeFrom = new SlotsSystem.Events.RemoveFrom(event, child, parent, slot, null);
                        if (slotModify.call() | removeFrom.call())
                            return;
                        child(slotModify.newChild().orElse(null));
                        event.getView().setCursor(nodeItem);
                    } else {
                        if (cursor.isSimilar(nodeItem)) {
                            // child -> [cursor]
                            // slotNode, _
                            SlotsSystem.Events.SlotModify slotModify = new SlotsSystem.Events.SlotModify(event, parent, slot, child, null);
                            SlotsSystem.Events.RemoveFrom removeFrom = new SlotsSystem.Events.RemoveFrom(event, child, parent, slot, null);
                            if (slotModify.call() | removeFrom.call())
                                return;
                            child(slotModify.newChild().orElse(null));
                            cursor.add();
                        } else if (cursorNode != null && slot.compatible(parent, cursorNode) && cursor.getAmount() == 1)  {
                            // cursorNode -> [slot], child -> [cursor]
                            // slotNode, cursorNode
                            SlotsSystem.Events.SlotModify slotModify = new SlotsSystem.Events.SlotModify(event, parent, slot, child, cursorNode);
                            SlotsSystem.Events.RemoveFrom removeFrom = new SlotsSystem.Events.RemoveFrom(event, child, parent, slot, cursorNode);
                            SlotsSystem.Events.InsertInto insertInto = new SlotsSystem.Events.InsertInto(event, cursorNode, parent, slot, child);
                            if (slotModify.call() | removeFrom.call() | insertInto.call())
                                return;
                            child(slotModify.newChild().orElse(null));
                            view.setCursor(nodeItem);
                        } else
                            return;
                    }
                }

                node.build();
                updateItems(plugin.persistenceManager().load(view.getCursor()).orElse(null));
                callTreeModify();
            }
        }
    }

    private final SokolPlugin plugin;
    private Locale locale;
    private PaperTreeNode node;

    private final Point2 center;
    private final Item[] items;
    private boolean modification;
    private boolean limited;
    private Consumer<PaperTreeNode> treeModifyCallback;

    public SlotViewPane(SokolPlugin plugin, int x, int y, int length, int height, Priority priority, Locale locale, PaperTreeNode node) {
        super(x, y, length, height, priority);
        this.plugin = plugin;
        this.locale = locale;
        this.node = node;
        center = new Point2(length / 2, height / 2);
        items = new Item[length * height];
        updateItems(null);
    }

    public SlotViewPane(SokolPlugin plugin, int length, int height, Locale locale, PaperTreeNode node) {
        super(length, height);
        this.plugin = plugin;
        this.locale = locale;
        this.node = node;
        center = new Point2(length / 2, height / 2);
        items = new Item[length * height];
        updateItems(null);
    }

    public SlotViewPane(SokolPlugin plugin, int x, int y, int length, int height, Locale locale, PaperTreeNode node) {
        super(x, y, length, height);
        this.plugin = plugin;
        this.locale = locale;
        this.node = node;
        center = new Point2(length / 2, height / 2);
        items = new Item[length * height];
        updateItems(null);
    }

    public SokolPlugin plugin() { return plugin; }

    public Locale locale() { return locale; }
    public SlotViewPane locale(Locale locale) { this.locale = locale; return this; }

    public PaperTreeNode node() { return node; }
    public SlotViewPane node(PaperTreeNode node) { this.node = node; return this; }

    public boolean modification() { return modification; }
    public SlotViewPane modification(boolean modification) { this.modification = modification; return this; }

    public boolean limited() { return limited; }
    public SlotViewPane limited(boolean limited) { this.limited = limited; return this; }

    public Consumer<PaperTreeNode> treeModifyCallback() { return treeModifyCallback; }
    public SlotViewPane treeModifyCallback(Consumer<PaperTreeNode> treeModifyCallback) { this.treeModifyCallback = treeModifyCallback; return this; }
    public void callTreeModify() {
        if (treeModifyCallback != null)
            treeModifyCallback.accept(node);
    }

    private ItemStack createItem(@Nullable PaperSlot slot, @Nullable PaperTreeNode parent, @Nullable PaperTreeNode node, @Nullable PaperTreeNode cursor) {
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

            return PaperUtils.modify(plugin.setting(plugin.invalidItem(), (n, d) -> n.get(ItemDescriptor.class, d), "slot_view", "gui", "slot", itemPath).createRaw(),
                    meta -> meta.displayName(Components.BLANK.append(slotText(slot))));
        } else {
            return PaperUtils.modify(item(node.asRoot()), meta -> {
                PaperUtils.addLore(meta, Components.BLANK.append(slotText(slot)));
                if (cursor != null && slot.compatible(parent, cursor)) {
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 0, true); // TODO replace with glint API
                }
            });
        }
    }

    private void put(Item item, int x, int y) {
        if (x <= length && y <= height)
            items[(y * length) + x] = item;
    }

    private void updateItems(@Nullable PaperTreeNode cursor, PaperSlot slot, PaperTreeNode parent, @Nullable PaperTreeNode node, int cx, int cy) {
        int x = cx + slot.offset().x(), y = cy + slot.offset().y();
        put(new Item(slot, parent, node, cursor), x, y);
        if (node != null) {
            for (var entry : node.slotChildren().entrySet()) {
                updateItems(cursor, entry.getValue().slot(), node, entry.getValue().child().orElse(null), x, y);
            }
        }
    }

    public void updateItems(@Nullable PaperTreeNode cursor) {
        Arrays.fill(items, null);

        boolean showInternal = true;
        for (var entry : node.slotChildren().entrySet()) {
            if (!entry.getValue().slot().internal() && entry.getValue().child().isPresent()) {
                showInternal = false;
                break;
            }
        }

        int x = center.x(), y = center.y();
        put(new Item(null, null, node, null), x, y);
        for (var entry : node.slotChildren().entrySet()) {
            PaperSlot slot = entry.getValue().slot();
            if (slot.internal() && !showInternal)
                continue;
            updateItems(cursor, slot, node, entry.getValue().child().orElse(null), x, y);
        }
    }

    private ItemStack item(@Nullable PaperTreeNode node) {
        return node == null
                ? plugin.invalidItem().createRaw()
                : node.system(PaperItemSystem.KEY)
                    .map(is -> is.create(locale).handle())
                    .orElse(PaperUtils.modify(plugin.invalidItem().createRaw(), meta ->
                            meta.displayName(Components.BLANK.append(node.value().name(locale)))));
    }

    private Component slotText(PaperSlot slot) {
        Collection<String> tags = slot.tags();
        Component[] components = new Component[tags.size()];
        int i = 0;
        for (String tag : tags) {
            components[i] = plugin.lc().safe(locale, "slot.meta.tag." + tag);
            ++i;
        }
        return plugin.lc().safe(locale, "slot.meta.name",
                "name", slot.name(locale),
                "tags", Component.join(plugin.lc().safe(locale, "slot.meta.tag_separator"), components));
    }

    private Collection<GuiItem> toItems() {
        Collection<GuiItem> result = new ArrayList<>();
        for (Item item : items) {
            if (item != null)
                result.add(item);
        }
        return result;
    }

    @Override
    public void display(InventoryComponent inv, int offX, int offY, int maxLength, int maxHeight) {
        for (int i = 0; i < items.length; i++) {
            GuiItem item = items[i];
            if (item == null || !item.isVisible())
                continue;
            inv.setItem(items[i], i % length, i / length);
        }
    }

    @Override
    public boolean click(Gui gui, InventoryComponent inv, InventoryClickEvent event, int slot, int offX, int offY, int maxLength, int maxHeight) {
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
        PaperTreeNode clickedNode = plugin.persistenceManager().load(clicked).orElse(null);
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
                updateItems(plugin.persistenceManager().load(event.getCursor()).orElse(null));
                gui.update();
            });
        }
    }

    @Override public @NotNull Collection<GuiItem> getItems() { return Arrays.asList(items); }
    @Override public @NotNull Collection<Pane> getPanes() { return new ArrayList<>(); }
    @Override public void clear() {}
}
