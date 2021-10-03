package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;

import java.util.Locale;

public interface ItemUser {
    Vector3 position();
    Vector3 direction();
    Locale locale();
}
