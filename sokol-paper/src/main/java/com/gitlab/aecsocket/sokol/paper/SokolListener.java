package com.gitlab.aecsocket.sokol.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/* package */ class SokolListener implements Listener {
    private final SokolPlugin plugin;

    public SokolListener(SokolPlugin plugin) {
        this.plugin = plugin;
    }

    public SokolPlugin plugin() { return plugin; }

    @EventHandler
    private void event(PlayerInteractEvent event) {

    }
}
