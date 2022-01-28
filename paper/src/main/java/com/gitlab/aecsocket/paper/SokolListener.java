package com.gitlab.aecsocket.paper;

import com.github.aecsocket.sokol.core.world.ItemSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.incendo.interfaces.paper.utils.PaperUtils;

/* package */ final class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onEvent(PlayerInteractEvent event) {
        // todo
    }
}
