package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Locale;

public interface EntityUser extends PaperUser {
    Entity handle();

    @Override default Location location() { return handle().getLocation(); }
    @Override default Locale locale() { return platform().defaultLocale(); }

    static EntityUser of(SokolPlugin platform, Entity entity) {
        return new EntityUser() {
            @Override public SokolPlugin platform() { return platform; }
            @Override public Entity handle() { return entity; }
        };
    }
}
