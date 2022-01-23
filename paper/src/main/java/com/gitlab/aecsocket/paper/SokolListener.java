package com.gitlab.aecsocket.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

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
