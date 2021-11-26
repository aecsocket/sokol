package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.paper.event.PaperItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.interfaces.core.view.InterfaceView;

/* package */ final class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onEvent(PlayerQuitEvent event) {
        plugin.removePlayerData(event.getPlayer());
    }

    private interface EventFactory {
        NodeEvent<PaperNode> create(PaperNode node, PaperItem item);
    }

    private void forward(@Nullable ItemStack stack, EventFactory... eventFactories) {
        plugin.persistence().safeLoad(stack).ifPresent(node -> {
            @SuppressWarnings("ConstantConditions")
            PaperItem item = plugin.wrap(stack);
            for (var eventFactory : eventFactories) {
                node.call(eventFactory.create(node, item));
            }
        });
    }

    @EventHandler
    private void onEvent(InventoryClickEvent event) {
        if (event instanceof InventoryCreativeEvent)
            return;
        if (
                event.getClickedInventory() == event.getView().getTopInventory()
                && !(event.getView().getTopInventory().getHolder() instanceof BlockInventoryHolder)
        ) return;
        // Hack to fix interfaces' bug
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        InterfaceView<?, ?> ifView = event.getInventory().getHolder() instanceof InterfaceView<?, ?> e ? e : null;

        PaperUser user = PlayerUser.user(plugin, player);
        forward(event.getCurrentItem(), (node, item) -> PaperItemEvent.SlotClick.of(plugin, node, user, item, event));
        forward(event.getCursor(), (node, item) -> PaperItemEvent.CursorClick.of(plugin, node, user, item, event));

        PlayerInventory inventory = player.getInventory();
        if (event.getSlot() == inventory.getHeldItemSlot()) {

        }

        if (
                ifView != null && !event.isCancelled()
                && event.getClickedInventory() == event.getView().getBottomInventory()
                && event.isShiftClick()
        ) {
            event.setCancelled(true);
            return;
        }

        if (!event.isCancelled() && ifView != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ifView::update);
        }
    }

    @EventHandler
    private void onEvent(InventoryDragEvent event) {
        // holding click counts as a drag, even if it's only in 1 slot
        InterfaceView<?, ?> ifView = event.getInventory().getHolder() instanceof InterfaceView<?, ?> e ? e : null;
        if (ifView != null) {
            for (int slot : event.getRawSlots()) {
                if (event.getView().getInventory(slot) == event.getView().getTopInventory()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        InventoryView view = event.getView();
        PaperUser user = PlayerUser.user(plugin, player);
        for (int rawSlot : event.getRawSlots()) {
            forward(view.getItem(rawSlot), (node, item) -> PaperItemEvent.SlotDrag.of(plugin, node, user, item, rawSlot, event));
        }

        if (!event.isCancelled() && ifView != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, ifView::update);
        }
    }
}
