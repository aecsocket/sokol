package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.paper.event.PaperItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
            PaperItem item = new PaperItem(stack);
            for (var eventFactory : eventFactories) {
                node.call(eventFactory.create(node, item));
            }
        });
    }

    @EventHandler
    private void onEvent(InventoryClickEvent event) {
        // Hack to fix interfaces' bug
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (
                event.getInventory().getHolder() instanceof InterfaceView
                && event.getClickedInventory() == event.getView().getBottomInventory()
                && event.isShiftClick()
        ) {
            event.setCancelled(true);
            return;
        }

        System.out.println("A");
        if (event instanceof InventoryCreativeEvent)
            return;

        PaperUser user = PlayerUser.user(plugin, player);
        System.out.println("B");
        forward(event.getCurrentItem(), (node, item) -> PaperItemEvent.SlotClick.of(node, user, item, event));
        forward(event.getCursor(), (node, item) -> PaperItemEvent.CursorClick.of(node, user, item, event));

        PlayerInventory inventory = player.getInventory();
        if (event.getSlot() == inventory.getHeldItemSlot()) {

        }
    }
}
