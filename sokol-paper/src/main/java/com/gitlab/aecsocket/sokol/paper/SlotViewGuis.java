package com.gitlab.aecsocket.sokol.paper;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.gitlab.aecsocket.sokol.paper.slotview.SlotViewPane;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public final class SlotViewGuis {
    private final SokolPlugin plugin;

    SlotViewGuis(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    public ChestGui create(SlotViewPane pane, Consumer<InventoryClickEvent> onGlobalClick) {
        // todo components
        ChestGui gui = new ChestGui(6, LegacyComponentSerializer.legacySection().serialize(pane.node().value().name(pane.locale())));
        Consumer<PaperTreeNode> treeModifyCallback = pane.treeModifyCallback();
        pane.treeModifyCallback(node -> {
            gui.update();
            if (treeModifyCallback != null)
                treeModifyCallback.accept(node);
        });
        gui.addPane(pane);
        gui.setOnGlobalClick(event -> {
            onGlobalClick.accept(event);
            if (!event.isCancelled())
                pane.handleGlobalClick(gui, event);
        });
        gui.setOnTopDrag(event -> event.setCancelled(true));
        return gui;
    }
}
