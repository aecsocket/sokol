package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

public interface ItemEvent<N extends Node.Scoped<N, ?, ?>, I extends Item> extends NodeEvent<N> {
    ItemUser user();
    ItemSlot<I> slot();
    I item();

    interface Hold<N extends Node.Scoped<N, ?, ?>, I extends Item> extends ItemEvent<N, I> {
        boolean sync();
        TaskContext context();
    }
}
