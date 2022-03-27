package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.sokol.core.FeatureInstance;
import com.github.aecsocket.sokol.core.TreeNode;

import java.util.Optional;

public interface TaskScheduler<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {
    @FunctionalInterface
    interface Task<N extends TreeNode.Scoped<N, ?, ?, ?, ?>, F extends FeatureInstance<?, ?, N>> {
        void run(F feature, TaskContext<N, F> ctx);
    }

    interface TaskContext<N extends TreeNode.Scoped<N, ?, ?, ?, ?>, F extends FeatureInstance<?, ?, N>> {
        Optional<N> now(N old);
        Optional<F> now(F old);
    }

    static <N extends TreeNode.Scoped<N, ?, ?, ?, ?>, F extends FeatureInstance<?, ?, N>> TaskContext<N, F> createContext(N node) {
        return new TaskContext<>() {
            @Override
            public Optional<N> now(N old) { return node.get(old.path()); }

            @Override
            @SuppressWarnings("unchecked")
            public Optional<F> now(F old) {
                return now(old.parent()).flatMap(now -> (Optional<F>) (now.feature(old.profile().type().id())));
            }
        };
    }

    <F extends FeatureInstance<?, ?, N>> int schedule(F feature, long delay, Task<N, F> task);

    void unschedule(int taskId);

    long readyAt();

    boolean ready();

    void delay(long ms);
}
