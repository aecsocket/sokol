package com.gitlab.aecsocket.sokol.core.util.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.feature.Feature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * An event concerning a {@link TreeNode}.
 */
public interface TreeEvent {
    /**
     * The node that the event occurred on.
     * @return The node.
     */
    TreeNode node();

    /**
     * Calls this event on the {@link #node()}.
     * @return If the event is a {@link Cancellable} and was cancelled.
     */
    default boolean call() {
        node().events().call(this);
        if (this instanceof Cancellable cancellable)
            return cancellable.cancelled();
        return false;
    }

    /**
     * An event which occurred due to a {@link Feature.Instance}.
     * @param <Y> The feature instance type.
     */
    interface SystemEvent<Y extends Feature.Instance> extends TreeEvent {
        /**
         * The system which caused the event.
         * @return The system.
         */
        Y system();

        @Override default TreeNode node() { return system().parent(); }
    }

    interface ItemEvent extends TreeEvent {
        ItemUser user();
        ItemSlot slot();

        boolean updated();

        void update(@Nullable Function<ItemStack, ItemStack> function);

        default void update() {
            update(null);
        }
    }

    abstract class BaseItemEvent implements ItemEvent {
        private Function<ItemStack, ItemStack> update;

        @Override public boolean updated() { return update != null; }

        @Override
        public void update(@Nullable Function<ItemStack, ItemStack> function) {
            if (update == null) {
                update = is -> {
                    ItemStack is2 = is.amount(slot().get()
                            .orElseThrow(() -> new IllegalStateException("Updating slot with no item")).amount());
                    if (user() instanceof PlayerUser player && player.inAnimation())
                        is2 = is2.hideUpdate();
                    return function == null ? is2 : function.apply(is2);
                };
            } else if (function != null) {
                var old = update;
                update = is -> {
                    is = old.apply(is);
                    return is == null ? null : function.apply(is);
                };
            }
        }

        @Override
        public boolean call() {
            boolean result = ItemEvent.super.call();
            if (update != null) {
                slot().set(node(), user().locale(), update);
                new Update(node(), user(), slot()).call();
            }
            return result;
        }
    }

    record Update(
            TreeNode node,
            ItemUser user,
            ItemSlot slot
    ) implements TreeEvent {}
}
