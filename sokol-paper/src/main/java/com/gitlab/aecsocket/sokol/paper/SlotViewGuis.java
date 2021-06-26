package com.gitlab.aecsocket.sokol.paper;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.gitlab.aecsocket.sokol.paper.slotview.SlotViewPane;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Handles the creation of GUIs which use {@link SlotViewPane}.
 */
public final class SlotViewGuis {
    private final SokolPlugin plugin;

    SlotViewGuis(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    /**
     * Creates a GUI from an existing pane.
     * <p>
     * Note that the created GUI's {@link com.github.stefvanschie.inventoryframework.gui.type.util.Gui#onGlobalClick} must <b>not</b>
     * be overwritten. Use this method's {@code onGlobalClick} parameter instead.
     * @param pane The pane.
     * @param onGlobalClick The function to run on global click.
     * @return The GUI.
     */
    public @NotNull ChestGui create(@NotNull SlotViewPane pane, @NotNull Consumer<InventoryClickEvent> onGlobalClick) {
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
