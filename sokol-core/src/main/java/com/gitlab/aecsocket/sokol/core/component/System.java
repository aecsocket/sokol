package com.gitlab.aecsocket.sokol.core.component;

import org.jetbrains.annotations.NotNull;

public interface System<B extends System.Base<?>> {
    interface Base<Y extends System<?>> {
        @NotNull String id();
        @NotNull Y create(Component component);
    }

    B base();
}
