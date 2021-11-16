package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;

public interface ItemEvent<N extends Node.Scoped<N, ?, ?>, I extends Item> extends VirtualItemEvent<N>, UserEvent<N> {
    ItemSlot<I> slot();
    @Override I item();

    interface Hold<N extends Node.Scoped<N, ?, ?>, I extends Item> extends ItemEvent<N, I> {
        boolean sync();
        TaskContext context();
    }
}
