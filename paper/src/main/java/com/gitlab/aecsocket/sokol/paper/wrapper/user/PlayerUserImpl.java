package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Player;

import java.util.Locale;

/* package */ class PlayerUserImpl<E extends Player> extends EntityUserImpl<E> implements PlayerUser {
    PlayerUserImpl(SokolPlugin plugin, E entity) {
        super(plugin, entity);
    }

    @Override public Locale locale() { return entity.locale(); }
}
