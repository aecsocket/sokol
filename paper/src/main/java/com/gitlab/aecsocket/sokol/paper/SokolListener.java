package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.incendo.interfaces.core.click.ClickHandler;
import org.incendo.interfaces.core.view.InterfaceView;
import org.incendo.interfaces.paper.PlayerViewer;
import org.incendo.interfaces.paper.type.ChestInterface;
import org.incendo.interfaces.paper.view.PlayerInventoryView;

/* package */ final class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onEvent(PlayerQuitEvent event) {
        plugin.removePlayerData(event.getPlayer());
    }

    @EventHandler
    private void onEvent(PlayerArmSwingEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        plugin.persistence().safeLoad(item).ifPresent(node -> {
            ChestInterface inf = ChestInterface.builder()
                    .rows(6)
                    .clickHandler(ClickHandler.cancel())
                    .addTransform(plugin.interfaces().nodeTreeTransform(node, PlayerUser.user(plugin, player)))
                    .build();
            //noinspection ConstantConditions
            inf.open(PlayerViewer.of(player), plugin.interfaces().nodeTreeTitle(player.locale(), item.getItemMeta().displayName()));
        });
    }

    @EventHandler
    private void onEvent(InventoryClickEvent event) {
        // Hack to fix interfaces' bug
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!(event.getInventory().getHolder() instanceof InterfaceView))
            return;
        if (event.isShiftClick() && event.getClickedInventory() == event.getView().getBottomInventory()) {
            event.setCancelled(true);
        }
    }
}
