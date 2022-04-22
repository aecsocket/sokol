package com.github.aecsocket.sokol.core.item;

import com.github.aecsocket.minecommons.core.Range;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

public interface ItemState {
    Component name();
    ItemState name(Component name);

    ItemState lore(Range.@Nullable Integer range, Locale locale, List<Component> lore);

    OptionalDouble durability();
    ItemState durability(double percent);
    ItemState repair();

    interface Scoped<
        T extends Scoped<T>
    > extends ItemState {
        @Override T name(Component name);

        @Override ItemState lore(Range.@Nullable Integer range, Locale locale, List<Component> lore);

        @Override T durability(double percent);
        @Override T repair();
    }
}
