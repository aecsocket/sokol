package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

record PlayerUserImpl(
        SokolPlugin platform,
        Player handle
) implements PlayerUser {}
