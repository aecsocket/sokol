package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface PaperUser extends ItemUser {
    @NotNull SokolPlugin platform();

    @NotNull Location location();

    @Override default @NotNull Vector3 position() { return PaperUtils.toCommons(location()); }
    @Override default @NotNull Vector3 direction() { return PaperUtils.toCommons(location().getDirection()); }
}
