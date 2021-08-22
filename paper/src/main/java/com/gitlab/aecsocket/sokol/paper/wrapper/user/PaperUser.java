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
        return new EntityUserImpl(platform, entity);
    }

    static LivingEntityUser living(SokolPlugin platform, LivingEntity entity) {
        return new LivingEntityUserImpl(platform, entity);
    }

    static PlayerUser player(SokolPlugin platform, Player entity) {
        return new PlayerUserImpl(platform, entity);
    }

    static LivingEntityUser anyLiving(SokolPlugin platform, LivingEntity living) {
        if (living instanceof Player player)
            return player(platform, player);
        return living(platform, living);
    }

    static EntityUser anyEntity(SokolPlugin platform, Entity entity) {
        if (entity instanceof Player player)
            return player(platform, player);
        if (entity instanceof LivingEntity living)
            return living(platform, living);
        return entity(platform, entity);
    }
}
