package com.gitlab.aecsocket.sokol.core.tree.event;

import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;

public final class ItemTreeEvent {
    private ItemTreeEvent() {}

    public interface Holding extends TreeEvent.ItemEvent {
        boolean sync();
        long elapsed();
        long delta();
        int iteration();
    }

    public interface SlotClickEvent extends TreeEvent.ItemEvent, Cancellable {
        boolean left();
        boolean right();
        boolean shift();
    }

    public interface ClickedSlotClickEvent extends SlotClickEvent {
        ItemSlot cursor();
    }

    public interface CursorSlotClickEvent extends SlotClickEvent {
        ItemSlot clicked();
    }

    public interface InputEvent extends TreeEvent.ItemEvent, Cancellable {
        InputType input();
    }
}
