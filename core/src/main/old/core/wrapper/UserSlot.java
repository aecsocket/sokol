package com.gitlab.aecsocket.sokol.core.wrapper;

public interface UserSlot<I extends Item> extends ItemSlot<I> {
    enum Position {
        HAND        (true, false),
        OFF_HAND    (true, false),
        HEAD        (false, true),
        CHEST       (false, true),
        LEGS        (false, true),
        FEET        (false, true);

        private final boolean hand;
        private final boolean armor;

        Position(boolean hand, boolean armor) {
            this.hand = hand;
            this.armor = armor;
        }

        public boolean hand() { return hand; }
        public boolean armor() { return armor; }
    }

    ItemUser user();
    Position position();
}
