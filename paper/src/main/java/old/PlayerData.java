package old;

import com.github.aecsocket.sokol.core.Tree;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;

import old.event.PaperItemEvent;
import old.impl.PaperNode;
import old.wrapper.slot.EquippedItemSlot;
import old.wrapper.user.PlayerUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {
    private record CacheEntry(ItemStack item, PaperNode node, Tree<PaperNode> ctx) {}

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
                CacheEntry entry = new CacheEntry(item, node, node.build(user));
                nodeCache.put(slot, entry);
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (entry) {
                    entry.ctx.call(new PaperItemEvent.Hold(node, user, EquippedItemSlot.slot(plugin, user, slot), plugin.wrap(item),
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
                var cache = entry.getValue();
                cache.ctx.call(new PaperItemEvent.Hold(cache.node, user, EquippedItemSlot.slot(plugin, user, entry.getKey()), plugin.wrap(entry.getValue().item),
                        false, ctx));
            }
        }
    }
}
