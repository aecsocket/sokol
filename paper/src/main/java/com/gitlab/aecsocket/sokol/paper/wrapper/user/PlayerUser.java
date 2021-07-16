package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Locale;

public interface PlayerUser extends LivingEntityUser, com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser {
    Player handle();

    @Override default Location location() { return handle().getEyeLocation(); }
    @Override default Locale locale() { return handle().locale(); }

    @Override default boolean sneaking() { return handle().isSneaking(); }
    @Override default boolean sprinting() { return handle().isSprinting(); }
}
