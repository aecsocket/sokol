package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
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

    interface InventoryClick<N extends Node.Scoped<N, ?, ?>, I extends Item> extends ItemEvent<N, I>, Cancellable {
        boolean left();
        boolean right();
        boolean shift();
    }

    interface SlotClick<N extends Node.Scoped<N, ?, ?>, I extends Item> extends InventoryClick<N, I> {
        ItemSlot<I> cursor();
    }

    interface CursorClick<N extends Node.Scoped<N, ?, ?>, I extends Item> extends InventoryClick<N, I> {
        ItemSlot<I> clicked();
    }

    interface SlotDrag<N extends Node.Scoped<N, ?, ?>, I extends Item> extends ItemEvent<N, I>, Cancellable {
        int rawSlot();
    }
}
