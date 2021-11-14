package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.LivingEntity;

public interface LivingUser extends EntityUser {
    @Override LivingEntity entity();

    static LivingUser user(SokolPlugin plugin, LivingEntity entity) {
        return new LivingUserImpl<>(plugin, entity);
    }
}
