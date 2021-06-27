package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public interface PlayerUser extends LivingEntityUser {
    @NotNull Player handle();

    @Override default @NotNull Location location() { return handle().getEyeLocation(); }
    @Override default @NotNull Locale locale() { return handle().locale(); }

    static PlayerUser of(SokolPlugin platform, Player entity) {
        return new PlayerUser() {
            @Override public @NotNull SokolPlugin platform() { return platform; }
            @Override public @NotNull Player handle() { return entity; }
        };
    }
}
