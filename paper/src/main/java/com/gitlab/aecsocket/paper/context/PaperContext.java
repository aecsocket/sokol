package com.gitlab.aecsocket.paper.context;

import com.github.aecsocket.sokol.core.context.Context;
import com.gitlab.aecsocket.paper.impl.PaperItemStack;
import com.gitlab.aecsocket.paper.world.PaperItemUser;
import com.gitlab.aecsocket.paper.world.slot.PaperItemSlot;

public interface PaperContext extends Context.User {
    @Override PaperItemUser user();

    interface Item extends PaperContext, Context.Item<PaperItemStack> {
        @Override PaperItemSlot slot();
    }

    static PaperContext context(PaperItemUser user) {
        return new PaperContextImpl(user);
    }

    static Item context(PaperItemUser user, PaperItemStack item, PaperItemSlot slot) {
        return new PaperContextImpl.Item(user, item, slot);
    }
}
