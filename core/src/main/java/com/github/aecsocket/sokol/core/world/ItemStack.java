package com.github.aecsocket.sokol.core.world;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

import com.github.aecsocket.sokol.core.BlueprintNode;

public interface ItemStack {
    String ITEM_LORE_SEPARATOR = "item.lore_separator";

    Optional<? extends BlueprintNode> asBlueprint();

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
    ItemStack addLore(Locale locale, List<Component> lore);

    OptionalDouble durability();
    ItemStack durability(double percent);
    ItemStack repair();

    interface Scoped<
        S extends Scoped<S, B>,
        B extends BlueprintNode.Scoped<B, ?, ?, ?>
    > extends ItemStack {
        @Override Optional<B> asBlueprint();

        @Override S amount(int amount);
        @Override S add(int amount);
        default @Override S add() { return add(1); }
        default @Override S subtract(int amount) { return add(-amount); }
        default @Override S subtract() { return add(-1); }

        @Override S name(Component name);
        @Override S lore(List<Component> lore);
        @Override S addLore(Locale locale, List<Component> lore);

        @Override S durability(double percent);
        @Override S repair();
    }
}
