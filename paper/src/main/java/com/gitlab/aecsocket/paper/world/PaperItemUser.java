package com.gitlab.aecsocket.paper.world;

import com.github.aecsocket.sokol.core.world.ItemUser;
import com.gitlab.aecsocket.paper.SokolPlugin;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface PaperItemUser extends ItemUser {
    Location location();

    interface OfEntity extends PaperItemUser {
        Entity entity();
    }

    interface OfLivingEntity extends OfEntity {
        @Override LivingEntity entity();
    }

    interface OfPlayer extends OfLivingEntity {
        @Override Player entity();
    }

    static OfEntity user(SokolPlugin plugin, Entity entity) {
        return new PaperItemUserImpl.OfEntity<>(plugin, entity);
    }

    static OfLivingEntity user(SokolPlugin plugin, LivingEntity entity) {
        return new PaperItemUserImpl.OfLivingEntity<>(plugin, entity);
    }

    static OfPlayer user(SokolPlugin plugin, Player entity) {
        return new PaperItemUserImpl.OfPlayer<>(plugin, entity);
    }
}
