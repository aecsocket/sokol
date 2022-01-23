package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.minecommons.core.effect.Effector;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;

import java.util.Locale;

public interface ItemUser extends Effector {
    Vector3 position();
    Vector3 direction();
    Locale locale();
}
