package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.scheduler.ThreadScheduler;
import com.gitlab.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import com.gitlab.aecsocket.sokol.paper.event.PaperEvent;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.EquipSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class SokolSchedulers {
    private final SokolPlugin plugin;
    private final PaperScheduler paperScheduler;
    private final ThreadScheduler threadScheduler;

    private final Map<UUID, Map<EquipmentSlot, PaperTreeNode>> nodeCache = new ConcurrentHashMap<>();

    public SokolSchedulers(SokolPlugin plugin) {
        this.plugin = plugin;
        paperScheduler = new PaperScheduler(plugin);
        threadScheduler = new ThreadScheduler(Executors.newFixedThreadPool(1));
    }

    public SokolPlugin plugin() { return plugin; }
    public PaperScheduler paperScheduler() { return paperScheduler; }
    public ThreadScheduler threadScheduler() { return threadScheduler; }

    void remove(Player player) {
        nodeCache.remove(player.getUniqueId());
    }

    void setup() {
        paperScheduler.run(Task.repeating(this::paperTick, Ticks.MSPT, 0));
        threadScheduler.run(Task.repeating(this::threadTick, 10, 0));
    }

    void stop() {
        paperScheduler.cancel();
        threadScheduler.cancel();
    }

    public void paperTick(TaskContext ctx) {
        synchronized (nodeCache) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                var playerCache = nodeCache.computeIfAbsent(player.getUniqueId(), u -> new EnumMap<>(EquipmentSlot.class));
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    PaperTreeNode node = plugin.persistenceManager().load(player.getInventory().getItem(slot)).orElse(null);
                    playerCache.put(slot, node);
                    if (node != null) {
                        new PaperEvent.Holding(
                                node, PlayerUser.of(plugin, player), EquipSlot.of(plugin, player, slot),
                                true, ctx.elapsed(), ctx.delta(), ctx.iteration()
                        ).call();
                    }
                }
            }
        }
    }

    public void threadTick(TaskContext ctx) {
        synchronized (nodeCache) {
            for (var entry : nodeCache.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (entry.getValue() == null)
                    continue;
                for (var cache : entry.getValue().entrySet()) {
                    if (cache.getValue() != null) {
                        new PaperEvent.Holding(
                                cache.getValue(), PlayerUser.of(plugin, player), EquipSlot.of(plugin, player, cache.getKey()),
                                false, ctx.elapsed(), ctx.delta(), ctx.iteration()
                        ).call();
                    }
                }
            }
        }
    }
}
