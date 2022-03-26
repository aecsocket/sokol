package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.sokol.core.FeatureInstance;

public interface TaskScheduler {
    @FunctionalInterface
    interface Task<F extends FeatureInstance<?, ?, ?>> {
        void run(F self, Context ctx);

        interface Context {
            // TODO something here
        }
    }

    <F extends FeatureInstance<?, ?, ?>> int schedule(F feature, long delay, Task<F> task);

    void unschedule(int taskId);

    long readyAt();

    boolean ready();

    void delay(long ms);
}
