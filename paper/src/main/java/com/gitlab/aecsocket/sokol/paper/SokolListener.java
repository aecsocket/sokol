package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.paper.slotview.SlotViewPane;
import com.gitlab.aecsocket.sokol.paper.system.SlotsSystem;
import com.gitlab.aecsocket.sokol.paper.system.impl.PaperItemSystem;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;
import java.util.function.Function;

/* package */ class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    private @SafeVarargs void handle(PaperTreeNode node, Function<PaperTreeNode, TreeEvent>... eventFactories) {
        for (var eventFactory : eventFactories) {
            node.events().call(eventFactory.apply(node));
        }
    }

    private @SafeVarargs void handle(ItemStack item, Function<PaperTreeNode, TreeEvent>... eventFactories) {
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

        HumanEntity human = event.getWhoClicked();
        Locale locale = plugin.locale(human);

        // TODO turn slot view/combine to event listeners
        ItemStack clicked = PaperUtils.normalize(event.getCurrentItem());
        PaperTreeNode clickedNode = plugin.persistenceManager().load(clicked).orElse(null);
        ItemStack cursor = PaperUtils.normalize(event.getCursor());
        PaperTreeNode cursorNode = plugin.persistenceManager().load(cursor).orElse(null);
        if (clickedNode == null)
            return;
        if (event.getClick() == ClickType.RIGHT && PaperUtils.empty(cursor) && plugin.setting(true, ConfigurationNode::getBoolean, "slot_view", "field_modify", "enabled")) {
            if (clicked.getAmount() > 1)
                return;
            int clickedSlot = event.getSlot();
            boolean clickedTop = event.getClickedInventory() == event.getView().getTopInventory();
            plugin.guis()
                    .create(new SlotViewPane(plugin, 9, 6, locale, clickedNode)
                            .modification(plugin.setting(true, ConfigurationNode::getBoolean, "slot_view", "field_modify", "modification"))
                            .limited(plugin.setting(true, ConfigurationNode::getBoolean, "slot_view", "field_modify", "limited"))
                            .treeModifyCallback(node ->
                                    event.setCurrentItem(node.<PaperItemSystem.Instance>system(ItemSystem.ID).orElseThrow().create(locale).handle())), evt -> {
                        if (Guis.isInvalid(evt, clickedTop, clickedSlot))
                            evt.setCancelled(true);
                    })
                    .show(human);
            event.setCancelled(true);
            return;
        }

        if (cursorNode != null && event.getClick() == ClickType.LEFT && plugin.setting(true, ConfigurationNode::getBoolean, "combine", "enabled")) {
            PaperTreeNode oldCursorNode = cursorNode.asRoot();
            if (clickedNode.combine(cursorNode, plugin.setting(true, ConfigurationNode::getBoolean, "combine", "limited"))) {
                if (
                        new SlotsSystem.Events.CombineChildOntoNode(event, clickedNode, cursorNode).call()
                        | new SlotsSystem.Events.CombineNodeOntoParent(event, oldCursorNode, clickedNode).call()
                )
                    return;
                event.setCancelled(true);
                int cursorAmount = cursor.getAmount();
                int clickedAmount = clicked.getAmount();
                if (cursorAmount >= clickedAmount) {
                    event.setCurrentItem(clickedNode.<PaperItemSystem.Instance>system(ItemSystem.ID)
                            .orElseThrow().create(locale).amount(clickedAmount).handle());
                    cursor.subtract(clickedAmount);
                } else {
                    event.getView().setCursor(clickedNode.<PaperItemSystem.Instance>system(ItemSystem.ID)
                                    .orElseThrow().create(locale).amount(cursorAmount).handle());
                    clicked.subtract(cursorAmount);
                }
            }
            return;
        }

        handle(clickedNode, n -> new PaperEvent.ClickedSlotClickEvent(plugin, event, n));
        if (cursorNode != null)
            handle(cursorNode, n -> new PaperEvent.CursorSlotClickEvent(plugin, event, n));
    }
}
