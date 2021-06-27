package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PaperSlot extends ItemSlot<PaperItemStack> {
    SokolPlugin plugin();

    @Override
    default Optional<PaperItemStack> get() {
        ItemStack stack = paperGet();
        return PaperUtils.empty(stack) ? Optional.empty() : Optional.of(new PaperItemStack(plugin(), stack));
    }

    ItemStack paperGet();

    @Override
    default void set(@Nullable PaperItemStack item) {
        paperSet(item == null ? new ItemStack(Material.AIR) : item.handle());
    }

    void paperSet(ItemStack item);

    static PaperSlot of(SokolPlugin plugin, Supplier<ItemStack> get, Consumer<ItemStack> set) {
        return new PaperSlot() {
            @Override public SokolPlugin plugin() { return plugin; }
            @Override public ItemStack paperGet() { return get.get(); }
            @Override public void paperSet(ItemStack item) { set.accept(item); }
        };
    }
}
