package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.sokol.core.Node;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public interface Item {
    Optional<?> node();

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

    interface Scoped<I extends Scoped<I, N>, N extends Node.Scoped<N, I, ?, ?>> extends Item {
        @Override Optional<N> node();

        @Override I amount(int amount);

        @Override I add(int amount);
        @Override default I add() { return add(1); }

        @Override default I subtract(int amount) { return add(-amount); }
        @Override default I subtract() { return subtract(1); }

        @Override I name(Component name);

        @Override I description(List<Component> description);
        @Override I addDescription(List<Component> description);

        @Override I durability(double percent);
        @Override I maxDurability();
    }
}
