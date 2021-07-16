package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface PaperUser extends ItemUser {
    SokolPlugin platform();

    Location location();

    @Override default Vector3 position() { return PaperUtils.toCommons(location()); }
    @Override default Vector3 direction() { return PaperUtils.toCommons(location().getDirection()); }

    static EntityUser entity(SokolPlugin platform, Entity entity) {
        return new EntityUser() {
            @Override public SokolPlugin platform() { return platform; }
            @Override public Entity handle() { return entity; }
        };
    }

    static LivingEntityUser living(SokolPlugin platform, LivingEntity entity) {
        return new LivingEntityUser() {
            @Override public SokolPlugin platform() { return platform; }
            @Override public LivingEntity handle() { return entity; }
        };
    }

    static PlayerUser player(SokolPlugin platform, Player entity) {
        return new PlayerUser() {
            @Override public SokolPlugin platform() { return platform; }
            @Override public Player handle() { return entity; }
        };
    }
}
