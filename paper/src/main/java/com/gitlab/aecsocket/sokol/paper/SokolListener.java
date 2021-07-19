package com.gitlab.aecsocket.sokol.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/* package */ class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    private @SafeVarargs void handle(PaperTreeNode node, Function<PaperTreeNode, PaperEvent>... eventFactories) {
        for (var eventFactory : eventFactories) {
            PaperEvent event = eventFactory.apply(node);
            event.call();
        }
    }

    private @SafeVarargs void handle(@Nullable ItemStack item, Function<PaperTreeNode, PaperEvent>... eventFactories) {
        plugin.persistenceManager().load(item).ifPresent(node -> handle(node, eventFactories));
    }

    @EventHandler
    private void event(PlayerQuitEvent event) {
        plugin.schedulers().remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void event(InventoryClickEvent event) {
        if (
                event.getClickedInventory() == event.getView().getTopInventory()
                && !(event.getView().getTopInventory().getHolder() instanceof BlockInventoryHolder)
        ) return;

        handle(event.getCurrentItem(), n -> new PaperEvent.ClickedSlotClickEvent(plugin, event, n));
        handle(event.getCursor(), n -> new PaperEvent.CursorSlotClickEvent(plugin, event, n));
    }
}
