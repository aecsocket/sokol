package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Locale;

/* package */ class EntityUserImpl<E extends Entity> implements EntityUser {
    protected final SokolPlugin plugin;
    protected final E entity;

    EntityUserImpl(SokolPlugin plugin, E entity) {
        this.plugin = plugin;
        this.entity = entity;
    }

    @Override public E entity() { return entity; }

    @Override public Location location() { return entity.getLocation(); }
    @Override public Locale locale() { return plugin.defaultLocale(); }
}
