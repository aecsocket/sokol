package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.Blueprint;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.Node;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public abstract class AbstractBlueprint<N extends Node.Scoped<N, ?, ?>> implements Blueprint<N> {
    private final String id;
    private final N node;

    public AbstractBlueprint(String id, N node) {
        this.id = id;
        this.node = node;
    }

    protected abstract SokolPlatform platform();

    @Override public String id() { return id; }
    @Override
    public Component name(Locale locale) {
        return platform().lc().safe(locale, "blueprint." + id);
    }

    public N node() { return node; }

    @Override
    public N build() {
        return node.copy();
    }
}
