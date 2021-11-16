package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;

public interface VirtualItemEvent<N extends Node.Scoped<N, ?, ?>> extends NodeEvent<N> {
    Item item();
}
