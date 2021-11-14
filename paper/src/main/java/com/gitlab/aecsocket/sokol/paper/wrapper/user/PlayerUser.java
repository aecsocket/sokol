package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Player;

public interface PlayerUser extends LivingUser {
    @Override Player entity();

    static PlayerUser user(SokolPlugin plugin, Player player) {
        return new PlayerUserImpl<>(plugin, player);
    }
}
