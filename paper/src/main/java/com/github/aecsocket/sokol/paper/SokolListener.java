package com.github.aecsocket.sokol.paper;

import com.github.aecsocket.sokol.core.nodeview.NodeView;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;

/* package */ final class SokolListener implements Listener {
    private static final NodeView.Options NODE_VIEW_DISABLED = new NodeView.Options(false, false);

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
        if (event instanceof InventoryCreativeEvent)
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        ItemStack cursorStack = event.getCursor();
        ItemStack clickedStack = event.getCurrentItem();

        if (!plugin.setting(true, ConfigurationNode::getBoolean, "node_view", "enabled"))
            return;
        if (cursorStack == null || cursorStack.getAmount() == 0) {
            if (event.isRightClick()) {
                plugin.persistence().load(clickedStack).ifPresent(clicked -> {
                    event.getView().setCursor(null);
                    event.setCancelled(true);
                    Locale locale = player.locale();
                    //noinspection ConstantConditions
                    plugin.createNodeView(locale, clicked.value().render(plugin.i18n(), locale), new NodeView<>(
                        event.getClickedInventory() == event.getView().getBottomInventory() || event.getView().getTopInventory().getHolder() instanceof BlockInventoryHolder
                            ? plugin.setting(NodeView.Options.DEFAULT, (n, d) -> n.get(NodeView.Options.class, d), "node_view")
                            : NODE_VIEW_DISABLED,
                        clicked.asTreeNode(PaperContext.context(
                            PaperItemUser.user(plugin, player),
                            new PaperItemStack(plugin, clickedStack),
                            PaperItemSlot.itemSlot(plugin, () -> null, s -> {}) // TODO
                        )),
                        clickedStack.getAmount(),
                        res -> event.setCurrentItem(res.asStack().handle())
                    ), event.getSlot())
                        .show(player);
                });
            }
        } else {

        }
    }
}
