package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.sokol.core.feature.inbuilt.ItemFeature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public interface ItemSlot {
    Optional<? extends ItemStack> get();
    void set(@Nullable ItemStack item);

    default void set(TreeNode node, Locale locale, Function<ItemStack, ItemStack> function) {
        set(function.apply(node.system(ItemFeature.KEY)
                .orElseThrow(() -> new IllegalStateException("Setting slot to node [" + node + "] which has no system [" + ItemFeature.ID + "]"))
                .create(locale)));
    }
}
