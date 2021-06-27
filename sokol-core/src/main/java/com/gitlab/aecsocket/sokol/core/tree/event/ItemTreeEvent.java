package com.gitlab.aecsocket.sokol.core.tree.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

public final class ItemTreeEvent {
    private ItemTreeEvent() {}

    public interface Holding<U extends ItemUser, S extends ItemSlot<?>> extends TreeEvent.ItemEvent<U, S> {
        boolean sync();
        long elapsed();
        long delta();
        int iteration();
    }

    public interface SlotClickEvent<U extends ItemUser, S extends ItemSlot<?>> extends TreeEvent.ItemEvent<U, S>, Cancellable {
        boolean left();
        boolean right();
        boolean shift();
    }

    public interface ClickedSlotClickEvent<U extends ItemUser, S extends ItemSlot<?>> extends SlotClickEvent<U, S> {
        S cursor();
    }

    public interface CursorSlotClickEvent<U extends ItemUser, S extends ItemSlot<?>> extends SlotClickEvent<U, S> {
        S clicked();
    }

    public interface HeldClickEvent<U extends ItemUser, S extends ItemSlot<?>> extends TreeEvent.ItemEvent<U, S>, Cancellable {
        enum Type {
            LEFT, RIGHT
        }

        Type type();
    }
}
