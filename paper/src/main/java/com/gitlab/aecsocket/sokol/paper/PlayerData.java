package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.Animation;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot.equip;

public final class PlayerData {
    private final SokolPlugin plugin;
    private final Player player;
    private final Map<EquipmentSlot, PaperTreeNode> nodeCache = new ConcurrentHashMap<>();
    private Animation.Instance animation;

    PlayerData(SokolPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public SokolPlugin plugin() { return plugin; }
    public Player player() { return player; }
    public Map<EquipmentSlot, PaperTreeNode> nodeCache() { return nodeCache; }
    public Animation.Instance animation() { return animation; }

    public void startAnimation(Animation.Instance instance) {
        animation = instance;
    }

    public void stopAnimation() {
        animation = null;
    }

    public void paperTick(TaskContext ctx) {
        if (player.isDead())
            return;

        if (animation != null) {
            animation.tick(ctx);
            if (animation.finished())
                animation = null;
        }

        synchronized (nodeCache) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                plugin.persistenceManager().load(player.getInventory().getItem(slot)).ifPresent(node -> {
                    nodeCache.put(slot, node);
                    new PaperEvent.Hold(
                            node, PaperUser.player(plugin, player), equip(plugin, player, slot),
                            true, ctx.elapsed(), ctx.delta(), ctx.iteration()
                    ).call();
                });
            }
        }
    }

    public void threadTick(TaskContext ctx) {
        synchronized (nodeCache) {
            for (var cache : nodeCache.entrySet()) {
                if (cache.getValue() != null) {
                    new PaperEvent.Hold(
                            cache.getValue(), PaperUser.player(plugin, player), equip(plugin, player, cache.getKey()),
                            false, ctx.elapsed(), ctx.delta(), ctx.iteration()
                    ).call();
                }
            }
        }
    }
}
