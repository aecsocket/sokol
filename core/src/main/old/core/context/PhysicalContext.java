package com.gitlab.aecsocket.sokol.core.context;

import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

/* package */ class PhysicalContext<I extends Item.Scoped<I, ?>> extends UserContext
        implements Context.User, Context.Slot<I> {
    private final I item;
    private final ItemSlot<I> slot;

    public PhysicalContext(ItemUser user, I item, ItemSlot<I> slot) {
        super(user);
        this.item = item;
        this.slot = slot;
    }

    @Override public I item() { return item; }
    @Override public ItemSlot<I> slot() { return slot; }
}
