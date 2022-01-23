package com.gitlab.aecsocket.sokol.core.context;

import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

import java.util.Locale;

public interface Context {
    Locale locale();

    interface User extends Context {
        ItemUser user();
    }

    interface WithItem<I extends Item> extends Context {
        I item();
    }

    interface Slot<I extends Item.Scoped<I, ?>> extends WithItem<I> {
        ItemSlot<I> slot();
    }

    static Context simple(Locale locale) {
        return new LocaleContext(locale);
    }

    static Context.User user(ItemUser user) {
        return new UserContext(user);
    }

    static <I extends Item.Scoped<I, ?>> PhysicalContext<I> physical(ItemUser user, I item, ItemSlot<I> slot) {
        return new PhysicalContext<>(user, item, slot);
    }
}
