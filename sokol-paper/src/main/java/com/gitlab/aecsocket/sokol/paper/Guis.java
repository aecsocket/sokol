package com.gitlab.aecsocket.sokol.paper;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.AnvilGui;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.paper.slotview.SlotViewPane;
import com.gitlab.aecsocket.sokol.paper.wrapper.ItemDescriptor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles the creation of {@link com.github.stefvanschie.inventoryframework.gui.type.util.Gui}s.
 */
public final class Guis {
    private final SokolPlugin plugin;

    Guis(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    /**
     * Creates a GUI from an existing pane.
     * <p>
     * Note that the created GUI's #onGlobalClick must <b>not</b>
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

    /**
     * Creates a text input GUI.
     * @param title The title of the GUI.
     * @param input The default component text input.
     * @param resultBuilder The builder for the resultant item.
     * @param callback The function to run when the confirmation (resultant) item is clicked.
     * @return The GUI.
     */
    public @NotNull AnvilGui textInput(Component title, Component input, Consumer<ItemMeta> resultBuilder, BiConsumer<InventoryClickEvent, String> callback) {
        // TODO components
        AnvilGui gui = new AnvilGui(LegacyComponentSerializer.legacySection().serialize(title));

        ItemStack placeholder = plugin.setting(plugin.invalidItem(), (n, d) -> n.get(ItemDescriptor.class, d), "text_input_placeholder")
                .createRaw();

        gui.setOnTopClick(evt -> evt.setCancelled(true));

        StaticPane firstPane = new StaticPane(1, 1);
        firstPane.addItem(new GuiItem(PaperUtils.modify(placeholder.clone(), meta -> {
            if (input != null)
                meta.displayName(Components.BLANK.append(input));
        }), evt -> evt.setCancelled(true)), 0, 0);
        gui.getFirstItemComponent().addPane(firstPane);

        StaticPane resultPane = new StaticPane(1, 1);
        resultPane.addItem(new GuiItem(PaperUtils.modify(placeholder.clone(), resultBuilder), evt -> {
            evt.setCancelled(true);
            evt.getView().close();
            callback.accept(evt, gui.getRenameText());
        }), 0, 0);
        gui.getResultComponent().addPane(resultPane);

        return gui;
    }

    /**
     * Gets if a particular inventory click is an illegal action, due to clicking a "busy" item.
     * @param event The event.
     * @param inTop If the "busy" item is in the top inventory.
     * @param clickedSlot The slot of the "busy" item.
     * @return If this action is illegal.
     */
    public static boolean isInvalid(InventoryClickEvent event, boolean inTop, int clickedSlot) {
        return event.getClickedInventory() == event.getView().getTopInventory() == inTop
                && (event.getSlot() == clickedSlot || (event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() == clickedSlot));
    }
}
