package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

public interface LivingEntityUser extends EntityUser {
    LivingEntity handle();

    @Override default Location location() { return handle().getEyeLocation(); }
    @Override default Locale locale() { return platform().defaultLocale(); }

    static LivingEntityUser of(SokolPlugin platform, LivingEntity entity) {
        return new LivingEntityUser() {
            @Override public SokolPlugin platform() { return platform; }
            @Override public LivingEntity handle() { return entity; }
        };
    }
}