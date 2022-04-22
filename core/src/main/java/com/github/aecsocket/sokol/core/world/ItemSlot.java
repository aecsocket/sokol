package com.github.aecsocket.sokol.core.world;

import com.github.aecsocket.sokol.core.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public interface ItemSlot<I extends ItemStack> {
    Optional<I> get();
    void set(@Nullable I item);
}
