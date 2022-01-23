package com.github.aecsocket.sokol.core.world;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.github.aecsocket.sokol.core.api.Blueprint;

public interface ItemStack {
    Optional<? extends Blueprint> asBlueprint();

    int amount();
    ItemStack amount(int amount);

    ItemStack add(int amount);
    default ItemStack add() { return add(1); }
    default ItemStack subtract(int amount) { return add(-amount); }
    default ItemStack subtract() { return add(-1); }

    Component name();
    ItemStack name(Component name);

    List<Component> lore();
    ItemStack lore(List<Component> lore);
    ItemStack addLore(List<Component> lore);

    OptionalDouble durability();
    ItemStack durability(double percent);
    ItemStack repair();

    interface Scoped<
            S extends Scoped<S, B>,
            B extends Blueprint.Scoped<B, ?, ?, ?>
    > extends ItemStack {
        @Override Optional<B> asBlueprint();

        @Override
        S amount(int amount);
        @Override
        S add(int amount);
        default @Override
        S add() { return add(1); }
        default @Override
        S subtract(int amount) { return add(-amount); }
        default @Override
        S subtract() { return add(-1); }

        @Override
        S name(Component name);
        @Override
        S lore(List<Component> lore);
        @Override
        S addLore(List<Component> lore);

        @Override
        S durability(double percent);
        @Override
        S repair();
    }
}
