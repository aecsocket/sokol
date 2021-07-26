package com.gitlab.aecsocket.sokol.paper;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.EquipSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.BiFunction;
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

    private void handleHands(Player player, BiFunction<PaperTreeNode, EquipSlot, PaperEvent> eventFactory) {
        handle(player.getInventory().getItemInMainHand(), n -> eventFactory.apply(n, PaperSlot.equip(plugin, player, EquipmentSlot.HAND)));
        handle(player.getInventory().getItemInOffHand(), n -> eventFactory.apply(n, PaperSlot.equip(plugin, player, EquipmentSlot.OFF_HAND)));
    }

    @EventHandler
    private void event(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        PlayerUser user = PaperUser.player(plugin, player);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            handle(inv.getItem(slot), n -> new PaperEvent.Equip(n, PaperSlot.equip(plugin, player, slot), user, null));
        }
    }

    @EventHandler
    private void event(PlayerQuitEvent event) {
        plugin.schedulers().remove(event.getPlayer());
    }

    @EventHandler
    private void event(PlayerDeathEvent event) {
        plugin.schedulers().remove(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void event(InventoryClickEvent event) {
        if (
                event.getClickedInventory() == event.getView().getTopInventory()
                && !(event.getView().getTopInventory().getHolder() instanceof BlockInventoryHolder)
        ) return;

        handle(event.getCurrentItem(), n -> new PaperEvent.ClickedSlotClick(plugin, event, n));
        handle(event.getCursor(), n -> new PaperEvent.CursorSlotClick(plugin, event, n));

        if (event.getWhoClicked() instanceof Player player) {
            PlayerInventory inv = player.getInventory();
            PlayerUser user = PaperUser.player(plugin, player);
            PaperSlot clickedSlot = PaperSlot.slot(plugin, event::getCurrentItem, event::setCurrentItem);
            if (event.getSlot() == inv.getHeldItemSlot()) {
                PaperSlot cursorSlot = PaperSlot.slot(plugin, event.getView()::getCursor, event.getView()::setCursor);
                handle(event.getCursor(),
                        n -> new PaperEvent.Equip(n, cursorSlot, user, clickedSlot));
                handle(event.getCurrentItem(),
                        n -> new PaperEvent.Unequip(n, clickedSlot, user, cursorSlot));
            }
        }
    }

    @EventHandler
    private void event(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot slot = switch (event.getSlotType()) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
        };
        PlayerInventory inv = player.getInventory();
        PlayerUser user = PaperUser.player(plugin, player);
        PaperSlot newSlot = PaperSlot.slot(plugin, event::getNewItem, s -> inv.setItem(slot, s));
        PaperSlot oldSlot = PaperSlot.slot(plugin, event::getOldItem, s -> inv.setItem(slot, s));
        handle(event.getNewItem(),
                n -> new PaperEvent.Equip(n, newSlot, user, oldSlot));
        handle(event.getOldItem(),
                n -> new PaperEvent.Unequip(n, oldSlot, user, newSlot));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void event(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        PlayerUser user = PaperUser.player(plugin, player);
        PaperSlot newSlot = PaperSlot.playerInventory(plugin, player, inv, event.getNewSlot(), event.getNewSlot());
        PaperSlot oldSlot = PaperSlot.playerInventory(plugin, player, inv, event.getPreviousSlot(), event.getNewSlot());
        handle(inv.getItem(event.getNewSlot()),
                n -> new PaperEvent.Equip(n, newSlot, user, oldSlot));
        handle(inv.getItem(event.getPreviousSlot()),
                n -> new PaperEvent.Unequip(n, oldSlot, user, newSlot));
    }

    @EventHandler
    private void event(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerUser user = PaperUser.player(plugin, player);
        handleHands(player, (n, s) -> new PaperEvent.BlockBreak(n, s, event, user));
    }

    @EventHandler
    private void event(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerUser user = PaperUser.player(plugin, player);
        handleHands(player, (n, s) -> new PaperEvent.BlockPlace(n, s, event, user));
    }
}
