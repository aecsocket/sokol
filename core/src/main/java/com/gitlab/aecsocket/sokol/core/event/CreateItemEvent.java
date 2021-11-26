package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;

public interface CreateItemEvent<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>>
        extends VirtualItemEvent<N, I>, LocalizedEvent<N> {}
