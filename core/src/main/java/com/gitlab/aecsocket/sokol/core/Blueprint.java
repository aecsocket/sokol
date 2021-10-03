package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;

public interface Blueprint<N extends Node> extends Keyed {
    N build();
}
