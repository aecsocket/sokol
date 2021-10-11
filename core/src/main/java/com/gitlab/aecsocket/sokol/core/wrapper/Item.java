package com.gitlab.aecsocket.sokol.core.wrapper;

import net.kyori.adventure.text.Component;

public interface Item {
    int amount();
    Item amount(int amount);

    Item add(int amount);
    default Item add() { return add(1); }

    default Item subtract(int amount) { return add(-amount); }
    default Item subtract() { return subtract(1); }

    Component name();
    Item name(Component name);
}
