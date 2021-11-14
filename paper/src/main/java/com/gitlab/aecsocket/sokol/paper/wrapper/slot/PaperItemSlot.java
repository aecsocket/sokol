package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PaperItemSlot extends ItemSlot<PaperItem> {
    @Nullable ItemStack bukkitGet();
    void bukkitSet(@Nullable ItemStack val);

    @Override
    default Optional<PaperItem> get() {
        return Optional.ofNullable(bukkitGet()).map(PaperItem::new);
    }

    @Override
    default void set(@Nullable PaperItem val) {
        bukkitSet(val == null ? null : val.handle());
    }

    static PaperItemSlot slot(Supplier<ItemStack> get, Consumer<ItemStack> set) {
        return new PaperItemSlotImpl(get, set);
    }
}
