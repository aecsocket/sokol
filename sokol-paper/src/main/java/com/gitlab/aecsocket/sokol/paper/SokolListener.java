package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.paper.system.PaperItemSystem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;

/* package */ class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void event(InventoryClickEvent event) {
        if (
                event.getClickedInventory() == event.getView().getTopInventory()
                && !(event.getView().getTopInventory().getHolder() instanceof BlockInventoryHolder)
        ) return;

        HumanEntity human = event.getWhoClicked();
        Locale locale = plugin.locale(human);

        ItemStack clicked = event.getCurrentItem();
        PaperTreeNode clickedNode = plugin.persistenceManager().load(clicked);
        if (clickedNode == null)
            return;
        if (event.getClick() == ClickType.RIGHT && plugin.setting(true, ConfigurationNode::getBoolean, "slot_view", "field_modify", "enabled")) {
            if (clicked.getAmount() > 1)
                return;
            int clickedSlot = event.getSlot();
            boolean clickedTop = event.getClickedInventory() == event.getView().getTopInventory();
            plugin.slotViewGuis()
                    .create(new SlotViewPane(plugin, 9, 6, locale, clickedNode)
                            .modification(plugin.setting(true, ConfigurationNode::getBoolean, "slot_view", "field_modify", "modification"))
                            .limited(plugin.setting(true, ConfigurationNode::getBoolean, "slot_view", "field_modify", "limited"))
                            .treeModifyCallback(node -> {
                                event.setCurrentItem(node.<PaperItemSystem.Instance>systemOf(ItemSystem.ID).create(locale).handle());
                            }), evt -> {
                        if (evt.getSlot() == clickedSlot && evt.getClickedInventory() == evt.getView().getTopInventory() == clickedTop)
                            evt.setCancelled(true);
                    })
                    .show(human);
            event.setCancelled(true);
            return;
        }

        ItemStack cursor = event.getCursor();
        PaperTreeNode cursorNode = plugin.persistenceManager().load(cursor);
        if (cursorNode == null)
            return;
        if (event.getClick() == ClickType.LEFT && plugin.setting(true, ConfigurationNode::getBoolean, "combine", "enabled")) {
            if (clickedNode.combine(cursorNode, plugin.setting(true, ConfigurationNode::getBoolean, "combine", "limited"))) {
                // TODO sound
                int cursorAmount = cursor.getAmount();
                int clickedAmount = clicked.getAmount();
                if (cursorAmount > clickedAmount) {
                    event.setCurrentItem(clickedNode.<PaperItemSystem.Instance>systemOf(ItemSystem.ID).create(locale).amount(clickedAmount).handle());
                    cursor.subtract(clickedAmount);
                } else {
                    event.getView().setCursor(clickedNode.<PaperItemSystem.Instance>systemOf(ItemSystem.ID).create(locale).amount(cursorAmount).handle());
                    clicked.subtract(cursorAmount);
                }
                event.setCancelled(true);
            }
        }
    }
}
