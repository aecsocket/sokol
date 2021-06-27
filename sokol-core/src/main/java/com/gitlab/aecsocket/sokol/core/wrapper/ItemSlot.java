package com.gitlab.aecsocket.sokol.core.wrapper;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public interface ItemSlot<I extends ItemStack> {
    Optional<I> get();
    void set(@Nullable I item);
}
