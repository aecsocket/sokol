package com.gitlab.aecsocket.sokol.core.tree.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
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

        boolean updateQueued();

        void queueUpdate(@Nullable Function<ItemStack, ItemStack> function);

        default void queueUpdate() {
            queueUpdate(null);
        }

        default void forceUpdate(Function<ItemStack, ItemStack> function) {
            if (user() instanceof PlayerUser player && player.inAnimation()) {
                var oldFunction = function;
                function = is -> oldFunction.apply(is.hideUpdate());
            }
            slot().set(node(), user().locale(), function);
        }
    }

    abstract class BaseItemEvent implements ItemEvent {
        private Function<ItemStack, ItemStack> updateQueued;

        @Override
        public boolean updateQueued() { return updateQueued != null; }

        @Override
        public void queueUpdate(@Nullable Function<ItemStack, ItemStack> function) {
            if (updateQueued == null) {
                Function<ItemStack, ItemStack> underlying = is -> is.amount(slot().get()
                        .orElseThrow(() -> new IllegalStateException("Updating slot with no item"))
                        .amount());
                updateQueued = function == null
                        ? underlying
                        : is -> function.apply(underlying.apply(is));
            } else {
                if (function != null) {
                    Function<ItemStack, ItemStack> old = updateQueued;
                    updateQueued = is -> function.apply(old.apply(is));
                }
            }
        }

        @Override
        public boolean call() {
            boolean result = ItemEvent.super.call();
            if (updateQueued != null) {
                node().build();
                forceUpdate(updateQueued);
            }
            return result;
        }

        @Override
        public void forceUpdate(Function<ItemStack, ItemStack> function) {
            var event = new PreUpdate(node(), user(), slot(), function);
            event.call();
            ItemEvent.super.forceUpdate(event.function);
            new PostUpdate(node(), user(), slot()).call();
        }
    }

    final class PreUpdate implements TreeEvent {
        private final TreeNode node;
        private final ItemUser user;
        private final ItemSlot slot;
        private Function<ItemStack, ItemStack> function;

        public PreUpdate(TreeNode node, ItemUser user, ItemSlot slot, Function<ItemStack, ItemStack> function) {
            this.node = node;
            this.user = user;
            this.slot = slot;
            this.function = function;
        }

        public TreeNode node() { return node; }
        public ItemUser user() { return user; }
        public ItemSlot slot() { return slot; }

        public void queueUpdate(Function<ItemStack, ItemStack> function) {
            var oldFunction = this.function;
            this.function = is -> function.apply(oldFunction.apply(is));
        }
    }

    record PostUpdate(
            TreeNode node,
            ItemUser user,
            ItemSlot slot
    ) implements TreeEvent {}
}
