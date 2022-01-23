package old.wrapper.slot;

import old.SokolPlugin;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/* package */ record PaperItemSlotImpl(
        SokolPlugin plugin,
        Supplier<@Nullable ItemStack> getter,
        Consumer<@Nullable ItemStack> setter
) implements PaperItemSlot {
    @Override public @Nullable ItemStack bukkitGet() { return getter.get(); }
    @Override public void bukkitSet(@Nullable ItemStack val) { setter.accept(val); }
}
