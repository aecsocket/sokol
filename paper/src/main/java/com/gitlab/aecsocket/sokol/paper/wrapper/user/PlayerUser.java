package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;

import java.util.Locale;

public interface PlayerUser extends LivingEntityUser, com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser {
    Player handle();

    @Override default Location location() { return handle().getEyeLocation(); }
    @Override default Locale locale() { return handle().locale(); }

    @Override default boolean sneaking() { return handle().isSneaking(); }
    @Override default boolean sprinting() { return handle().isSprinting(); }
    @Override default boolean leftHanded() { return handle().getMainHand() == MainHand.LEFT; }

    @Override default boolean inAnimation() { return platform().schedulers().playerData(handle()).animation() != null; }
}
