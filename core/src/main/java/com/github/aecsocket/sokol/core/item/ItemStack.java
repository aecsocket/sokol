package com.github.aecsocket.sokol.core.item;

import java.util.Optional;

import com.github.aecsocket.sokol.core.BlueprintNode;

public interface ItemStack<T extends ItemState> {
    String ITEM_LORE_SEPARATOR = "item.lore_separator";

    Optional<? extends BlueprintNode> asBlueprint();

    int amount();
    ItemStack<T> amount(int amount);

    ItemStack<T> add(int amount);
    default ItemStack<T> add() { return add(1); }
    default ItemStack<T> subtract(int amount) { return add(-amount); }
    default ItemStack<T> subtract() { return add(-1); }

    T state();
    ItemStack<T> state(T state);

    interface Scoped<
        T extends ItemState.Scoped<T>,
        S extends Scoped<T, S, B>,
        B extends BlueprintNode.Scoped<B, ?, ?, ?>
    > extends ItemStack<T> {
        @Override Optional<B> asBlueprint();

        @Override S amount(int amount);
        @Override S add(int amount);
        default @Override S add() { return add(1); }
        default @Override S subtract(int amount) { return add(-amount); }
        default @Override S subtract() { return add(-1); }

        @Override S state(T state);
    }
}
