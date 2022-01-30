package com.github.aecsocket.sokol.paper.context;

import java.util.Locale;

import com.github.aecsocket.sokol.paper.PaperItemStack;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import com.github.aecsocket.sokol.paper.world.slot.PaperItemSlot;

/* package */ class PaperContextImpl implements PaperContext {
    private final PaperItemUser user;

    public PaperContextImpl(PaperItemUser user) {
        this.user = user;
    }

    @Override public PaperItemUser user() { return user; }
    @Override public Locale locale() { return user.locale(); }

    static class Item extends PaperContextImpl implements PaperContext.Item {
        private final PaperItemStack item;
        private final PaperItemSlot slot;

        public Item(PaperItemUser user, PaperItemStack item, PaperItemSlot slot) {
            super(user);
            this.item = item;
            this.slot = slot;
        }

        @Override public PaperItemStack item() { return item; }
        @Override public PaperItemSlot slot() { return slot; }
    }
}
