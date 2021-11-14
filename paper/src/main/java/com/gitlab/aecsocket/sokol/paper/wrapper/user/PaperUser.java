package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface PaperUser extends ItemUser {
    Location location();

    @Override
    default Vector3 position() {
        return PaperUtils.toCommons(location());
    }

    @Override
    default Vector3 direction() {
        return PaperUtils.toCommons(location().getDirection());
    }
}
