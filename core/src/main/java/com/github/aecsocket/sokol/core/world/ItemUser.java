package com.github.aecsocket.sokol.core.world;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import net.kyori.adventure.audience.Audience;

import java.util.Locale;

public interface ItemUser extends Effector, Audience {
    Locale locale();

    Vector3 position();

    Vector3 direction();
}
