package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.minecommons.core.InputType;
import com.github.aecsocket.minecommons.core.event.Cancellable;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.sokol.core.TreeNode;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.world.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Function;

public interface ItemEvent<N extends TreeNode.Scoped<N, ?, ?, ?, S>, S extends ItemStack.Scoped<S, ?>> extends NodeEvent<N> {
    @FunctionalInterface
    interface Mapper<S extends ItemStack.Scoped<S, ?>> extends Function<S, S> {}

    void updateItem(Function<Mapper<S>, Mapper<S>> mapper);

    abstract class Base<N extends TreeNode.Scoped<N, ?, ?, ?, S>, S extends ItemStack.Scoped<S, ?>> implements ItemEvent<N, S> {
        private @Nullable Mapper<S> mapper;

        @Override
        public void updateItem(Function<@Nullable Mapper<S>, Mapper<S>> mapper) {
            this.mapper = mapper.apply(this.mapper == null ? is -> is : this.mapper);
        }

        @Override
        public boolean call() {
            boolean cancelled = ItemEvent.super.call();
            if (!cancelled) {
                if (node().context() instanceof Context.Item<S> context) {
                    context.slot().set();
                }
            }
            return cancelled;
        }
    }

    interface Hold<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends ItemEvent<N, ItemStack> {}

    interface Input<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends ItemEvent<N>, Cancellable {
        InputType input();
    }

    interface GameClick<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends ItemEvent<N>, Cancellable {
        Optional<Vector3> clickedPos();
    }

    interface SwapFrom<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {}
}
