package com.github.aecsocket.sokol.paper.context;

import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.paper.PaperItemStack;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;

public interface PaperContext extends Context.User {
    @Override PaperItemUser user();

    interface Item extends PaperContext, Context.Item<PaperItemStack> {
        @Override PaperItemSlot slot();
    }

    static Item context(PaperItemUser user, PaperItemStack item, PaperItemSlot slot) {
        return new PaperContextImpl.Item(user, item, slot);
    }
}
