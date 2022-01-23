package com.gitlab.aecsocket.sokol.core.context;

import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

import java.util.Locale;

/* package */ class UserContext implements Context.User {
    private final ItemUser user;

    public UserContext(ItemUser user) {
        this.user = user;
    }

    @Override public ItemUser user() { return user; }
    @Override public Locale locale() { return user.locale(); }
}
