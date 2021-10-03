package com.gitlab.aecsocket.sokol.core.wrapper;

public interface UserSlot extends ItemSlot {
    ItemUser user();
    boolean inHand();
    boolean inArmor();
}
