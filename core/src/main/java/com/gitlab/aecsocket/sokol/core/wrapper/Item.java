package com.gitlab.aecsocket.sokol.core.wrapper;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.OptionalDouble;

public interface Item {
    int amount();
    Item amount(int amount);

    Item add(int amount);
    default Item add() { return add(1); }

    default Item subtract(int amount) { return add(-amount); }
    default Item subtract() { return subtract(1); }

    Component name();
    Item name(Component name);

    List<Component> description();
    Item description(List<Component> description);
    Item addDescription(List<Component> description);

    OptionalDouble durability();
    Item durability(double percent);
    Item maxDurability();
}
