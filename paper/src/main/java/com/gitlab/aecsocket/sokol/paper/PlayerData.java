package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.paper.event.PaperItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.EquippedItemSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {
    private record CacheEntry(ItemStack item, PaperNode node) {}

    private final SokolPlugin plugin;
    private final Player player;
    private final Map<EquipmentSlot, CacheEntry> nodeCache = new ConcurrentHashMap<>();
    private int lastSlot;

    PlayerData(SokolPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    void paperTick(TaskContext ctx) {
        if (player.isDead())
            return;

        PlayerInventory inventory = player.getInventory();
        lastSlot = inventory.getHeldItemSlot();
        PlayerUser user = PlayerUser.user(plugin, player);
        for (var slot : EquipmentSlot.values()) {
            ItemStack item = inventory.getItem(slot);
            if (item == null)
                continue;
            plugin.persistence().safeLoad(item).ifPresentOrElse(node -> {
                CacheEntry entry = new CacheEntry(item, node);
                nodeCache.put(slot, entry);
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (entry) {
                    node.call(new PaperItemEvent.Hold(node, user, EquippedItemSlot.slot(user, slot), new PaperItem(item),
                            true, ctx));
                }
            }, () -> nodeCache.remove(slot));
        }
    }

    void threadTick(TaskContext ctx) {
        if (player.getInventory().getHeldItemSlot() != lastSlot)
            return; // if slot swapped, don't run hold tasks

        PlayerUser user = PlayerUser.user(plugin, player);
        for (var entry : nodeCache.entrySet()) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (entry) {
                PaperNode node = entry.getValue().node;
                node.call(new PaperItemEvent.Hold(node, user, EquippedItemSlot.slot(user, entry.getKey()), new PaperItem(entry.getValue().item),
                        false, ctx));
            }
        }
    }
}
