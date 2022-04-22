package com.github.aecsocket.sokol.core.world;

import com.github.aecsocket.sokol.core.item.ItemStack;

public interface EquipSlot<I extends ItemStack> extends ItemSlot<I> {
    enum Position {
        MAIN_HAND   (true, false),
        OFF_HAND    (true, false),
        HEAD        (false, true),
        CHEST       (false, true),
        LEGS        (false, true),
        FEET        (false, true);
        
        private final boolean hand;
        private final boolean worn;

        Position(boolean hand, boolean worn) {
            this.hand = hand;
            this.worn = worn;
        }

        public boolean hand() { return hand; }
        public boolean worn() { return worn; }
    }

    Position position();
}
