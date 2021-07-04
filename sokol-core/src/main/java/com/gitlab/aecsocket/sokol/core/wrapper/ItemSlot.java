package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public interface ItemSlot {
    Optional<? extends ItemStack> get();
    void set(@Nullable ItemStack item);

    default void set(@NotNull TreeNode node, @NotNull Locale locale) {
        ItemSystem.Instance itemSystem = (ItemSystem.Instance) node.systems().get(ItemSystem.ID);
        set(itemSystem.create(locale));
    }
}
