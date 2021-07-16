package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

public interface LivingEntityUser extends EntityUser {
    LivingEntity handle();

    @Override default Location location() { return handle().getEyeLocation(); }
    @Override default Locale locale() { return platform().defaultLocale(); }
}
