package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;

public interface CreateItemEvent<N extends Node.Scoped<N, ?, ?>> extends VirtualItemEvent<N>, LocalizedEvent<N> {}
