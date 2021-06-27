package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public interface LivingEntityUser extends EntityUser {
    @NotNull LivingEntity handle();

    @Override default @NotNull Location location() { return handle().getEyeLocation(); }
    @Override default @NotNull Locale locale() { return platform().defaultLocale(); }

    static LivingEntityUser of(SokolPlugin platform, LivingEntity entity) {
        return new LivingEntityUser() {
            @Override public @NotNull SokolPlugin platform() { return platform; }
            @Override public @NotNull LivingEntity handle() { return entity; }
        };
    }
}
