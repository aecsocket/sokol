package com.github.aecsocket.sokol.core.context;

import java.util.Locale;

import com.github.aecsocket.sokol.core.world.ItemSlot;
import com.github.aecsocket.sokol.core.world.ItemStack;
import com.github.aecsocket.sokol.core.world.ItemUser;

public interface Context {
    Locale locale();

    static Context context(Locale locale) {
        return new BasicContextImpl(locale);
    }

    interface User extends Context {
        ItemUser user();
    }

    interface Item<I extends ItemStack> extends User {
        I item();
        ItemSlot<I> slot();
    }
}
