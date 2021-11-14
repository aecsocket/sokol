package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public interface EntityUser extends PaperUser, ForwardingAudience {
    Entity entity();

    @Override default @NotNull Iterable<? extends Audience> audiences() { return Collections.singleton(entity()); }

    static EntityUser user(SokolPlugin plugin, Entity entity) {
        return new EntityUserImpl<>(plugin, entity);
    }

    static EntityUser autoUser(SokolPlugin plugin, Entity entity) {
        if (entity instanceof Player player)
            return PlayerUser.user(plugin, player);
        if (entity instanceof LivingEntity living)
            return LivingUser.user(plugin, living);
        return user(plugin, entity);
    }
}
