package com.gitlab.aecsocket.paper.world.slot;

import com.gitlab.aecsocket.paper.SokolPlugin;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/* package */ final class PaperItemSlotImpl implements PaperItemSlot {
    private final SokolPlugin plugin;
    private final Supplier<@Nullable ItemStack> get;
    private final Consumer<@Nullable ItemStack> set;

    public PaperItemSlotImpl(SokolPlugin plugin, Supplier<@Nullable ItemStack> get, Consumer<@Nullable ItemStack> set) {
        this.plugin = plugin;
        this.get = get;
        this.set = set;
    }

    @Override public SokolPlugin plugin() { return plugin; }
    @Override public @Nullable ItemStack getRaw() { return get.get(); }
    @Override public void setRaw(@Nullable ItemStack item) { set.accept(item); }
}
