package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public interface EntityUser extends PaperUser {
    @NotNull Entity handle();

    @Override default @NotNull Location location() { return handle().getLocation(); }
    @Override default @NotNull Locale locale() { return platform().defaultLocale(); }

    static EntityUser of(SokolPlugin platform, Entity entity) {
        return new EntityUser() {
            @Override public @NotNull SokolPlugin platform() { return platform; }
            @Override public @NotNull Entity handle() { return entity; }
        };
    }
}
