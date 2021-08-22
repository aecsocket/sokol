package com.gitlab.aecsocket.sokol.paper.wrapper.user;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.LivingEntity;

record LivingEntityUserImpl(
        SokolPlugin platform,
        LivingEntity handle
) implements LivingEntityUser {}
