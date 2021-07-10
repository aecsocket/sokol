package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Locale;

public interface PlayerUser extends LivingEntityUser {
    Player handle();

    @Override default Location location() { return handle().getEyeLocation(); }
    @Override default Locale locale() { return handle().locale(); }

    static PlayerUser of(SokolPlugin platform, Player entity) {
        return new PlayerUser() {
            @Override public SokolPlugin platform() { return platform; }
            @Override public Player handle() { return entity; }
        };
    }
}
