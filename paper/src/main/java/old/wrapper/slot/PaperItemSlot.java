package old.wrapper.slot;

import old.SokolPlugin;
import old.wrapper.PaperItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.aecsocket.sokol.core.wrapper.ItemSlot;

public interface PaperItemSlot extends ItemSlot<PaperItem> {
    SokolPlugin plugin();
    @Nullable ItemStack bukkitGet();
    void bukkitSet(@Nullable ItemStack val);

    @Override
    default Optional<PaperItem> get() {
        ItemStack stack = bukkitGet();
        return stack == null || stack.getType() == Material.AIR
                ? Optional.empty()
                : Optional.ofNullable(bukkitGet()).map(plugin()::wrap);
    }

    @Override
    default void set(@Nullable PaperItem val) {
        bukkitSet(val == null ? null : val.handle());
    }

    static PaperItemSlot slot(SokolPlugin plugin, Supplier<ItemStack> get, Consumer<ItemStack> set) {
        return new PaperItemSlotImpl(plugin, get, set);
    }
}
