package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;

public interface ItemEvent<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>>
        extends VirtualItemEvent<N, I>, UserEvent<N> {
    ItemSlot<I> slot();

    interface Hold<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>> extends ItemEvent<N, I> {
        boolean sync();
        TaskContext context();
    }

    interface InventoryClick<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>> extends ItemEvent<N, I>, Cancellable {
        boolean left();
        boolean right();
        boolean shift();
    }

    interface SlotClick<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>> extends InventoryClick<N, I> {
        ItemSlot<I> cursor();
    }

    interface CursorClick<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>> extends InventoryClick<N, I> {
        ItemSlot<I> clicked();
    }

    interface SlotDrag<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>> extends ItemEvent<N, I>, Cancellable {
        int rawSlot();
    }
}
