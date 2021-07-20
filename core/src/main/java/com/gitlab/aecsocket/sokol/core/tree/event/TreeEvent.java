package com.gitlab.aecsocket.sokol.core.tree.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
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
     * An event which occurred due to a {@link System.Instance}.
     * @param <Y> The system instance type.
     */
    interface SystemEvent<Y extends System.Instance> extends TreeEvent {
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

        void queueUpdate(@Nullable Function<ItemStack, ItemStack> function);

        default void queueUpdate() {
            queueUpdate(null);
        }

        default void forceUpdate(Function<ItemStack, ItemStack> function) {
            slot().set(node(), user().locale(), function);
        }
    }
}
