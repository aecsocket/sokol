package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.nodeview.NodeView;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/* package */ final class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onEvent(PlayerInteractEvent event) {
        PaperEvents.forInventory(plugin, event.getPlayer(),
            node -> new PaperEvents.GameClick(node, event));
    }

    @EventHandler
    private void onEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getClickedInventory() == event.getView().getTopInventory())
            return;
        ItemStack cursorStack = event.getCursor();
        ItemStack clickedStack = event.getCurrentItem();
        if (cursorStack == null || cursorStack.getAmount() == 0) {
            if (event.isRightClick()) {
                plugin.persistence().load(clickedStack).ifPresent(clicked -> {
                    event.getView().setCursor(null);
                    event.setCancelled(true);
                    Locale locale = player.locale();
                    //noinspection ConstantConditions
                    plugin.createNodeView(locale, clicked.value().render(plugin.i18n(), locale), new NodeView<>(
                        plugin.setting(NodeView.Options.DEFAULT, (n, d) -> n.get(NodeView.Options.class, d), "node_view"),
                        clicked.asTreeNode(PaperContext.context(
                            PaperItemUser.user(plugin, player),
                            new PaperItemStack(plugin, clickedStack),
                            PaperItemSlot.itemSlot(plugin, () -> null, s -> {}) // TODO
                        )),
                        clickedStack.getAmount(),
                        res -> {} // TODO
                    ), event.getSlot())
                        .show(player);
                });
            }
        } else {

        }
    }
}
