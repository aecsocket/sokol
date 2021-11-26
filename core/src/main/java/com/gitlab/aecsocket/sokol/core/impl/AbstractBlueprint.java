package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.Blueprint;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;

public abstract class AbstractBlueprint<N extends Node.Scoped<N, ?, ?, ?>> implements Blueprint<N> {
    private final String id;
    private final N node;

    public AbstractBlueprint(String id, N node) {
        Keyed.validate(id);
        this.id = id;
        this.node = node;
    }

    protected abstract SokolPlatform platform();

    @Override public String id() { return id; }

    public N node() { return node; }

    @Override
    public N create() {
        return node.asRoot();
    }
}
