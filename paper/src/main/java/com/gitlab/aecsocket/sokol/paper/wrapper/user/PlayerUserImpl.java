package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.minecommons.core.effect.Effector;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Locale;

/* package */ class PlayerUserImpl<E extends Player> extends EntityUserImpl<E> implements PlayerUser {
    private final Iterable<? extends Effector> effectors = Collections.singleton(plugin.effectors().ofPlayer(entity));

    PlayerUserImpl(SokolPlugin plugin, E entity) {
        super(plugin, entity);
    }

    @Override
    public Iterable<? extends Effector> effectors() { return effectors; }

    @Override public Locale locale() { return entity.locale(); }
}
