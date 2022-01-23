package com.github.aecsocket.sokol.core.world;

import com.gitlab.aecsocket.minecommons.core.effect.Effector;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import net.kyori.adventure.audience.Audience;

import java.util.Locale;

public interface ItemUser extends Effector, Audience {
    Locale locale();

    Vector3 position();

    Vector3 direction();
}
