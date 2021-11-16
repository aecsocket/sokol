package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

import java.util.Locale;

public interface UserEvent<N extends Node.Scoped<N, ?, ?>> extends LocalizedEvent<N> {
    ItemUser user();

    @Override default Locale locale() { return user().locale(); }
}
