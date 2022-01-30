package com.github.aecsocket.sokol.paper.world.slot;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.aecsocket.sokol.paper.SokolPlugin;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

/* package */ record PaperItemSlotImpl(
    SokolPlugin plugin,
    Supplier<@Nullable ItemStack> toGet,
    Consumer<@Nullable ItemStack> toSet
) implements PaperItemSlot {
    @Override
    public @Nullable ItemStack raw() {
        return toGet.get();
    }

    @Override
    public void raw(@Nullable ItemStack item) {
        toSet.accept(item);
    }
}
