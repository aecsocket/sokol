package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/* package */ class LivingUserImpl<E extends LivingEntity> extends EntityUserImpl<E> implements LivingUser {
    LivingUserImpl(SokolPlugin plugin, E entity) {
        super(plugin, entity);
    }

    @Override public Location location() { return entity.getEyeLocation(); }
}
