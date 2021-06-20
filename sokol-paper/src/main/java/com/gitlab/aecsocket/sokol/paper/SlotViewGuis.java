package com.gitlab.aecsocket.sokol.paper;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SlotViewGuis {
    private final SokolPlugin plugin;

    SlotViewGuis(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    public ChestGui create(PaperTreeNode tree, Locale locale, Function<Gui, SlotViewPane> paneFactory) {
        // todo components
        ChestGui gui = new ChestGui(6, LegacyComponentSerializer.legacySection().serialize(tree.value().name(locale)));
        SlotViewPane pane = paneFactory.apply(gui);
        Consumer<PaperTreeNode> treeModifyCallback = pane.treeModifyCallback();
        pane.treeModifyCallback(node -> {
            pane.updateGui(gui);
            if (treeModifyCallback != null)
                treeModifyCallback.accept(node);
        });
        gui.addPane(pane);
        gui.setOnGlobalClick(event -> pane.handleGlobalClick(gui, event));
        gui.setOnTopDrag(event -> event.setCancelled(true));
        return gui;
    }
}
