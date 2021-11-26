package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;

public interface VirtualItemEvent<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>>
        extends NodeEvent<N> {
    I item();
}
