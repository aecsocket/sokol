package com.gitlab.aecsocket.sokol.core.tree.event;

import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;

public final class ItemTreeEvent {
    private ItemTreeEvent() {}

    public interface Hold extends TreeEvent.ItemEvent {
        boolean sync();
        long elapsed();
        long delta();
        int iteration();
    }

    public interface SlotClick extends TreeEvent.ItemEvent, Cancellable {
        boolean left();
        boolean right();
        boolean shift();
    }

    public interface ClickedSlotClick extends SlotClick {
        ItemSlot cursor();
    }

    public interface CursorSlotClick extends SlotClick {
        ItemSlot clicked();
    }

    public interface Input extends TreeEvent.ItemEvent, Cancellable {
        InputType input();
    }

    public interface Equip extends TreeEvent.ItemEvent, Cancellable {
        ItemSlot oldSlot();
    }

    public interface Unequip extends TreeEvent.ItemEvent, Cancellable {
        ItemSlot newSlot();
    }

    public interface BlockBreak extends TreeEvent.ItemEvent, Cancellable {
        Vector3 position();
    }

    public interface BlockPlace extends TreeEvent.ItemEvent, Cancellable {
        Vector3 position();
    }

    public interface ShowItem extends TreeEvent.ItemEvent, Cancellable {}
}
