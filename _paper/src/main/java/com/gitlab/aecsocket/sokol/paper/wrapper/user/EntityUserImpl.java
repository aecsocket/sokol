package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Entity;

record EntityUserImpl(
        SokolPlugin platform,
        Entity handle
) implements EntityUser {}
