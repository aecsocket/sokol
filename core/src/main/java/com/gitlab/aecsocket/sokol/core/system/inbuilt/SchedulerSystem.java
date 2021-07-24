package com.gitlab.aecsocket.sokol.core.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import io.leangen.geantyref.TypeToken;

import java.util.*;

public abstract class SchedulerSystem<E extends ItemTreeEvent.Hold> extends AbstractSystem {
    /** The system ID. */
    public static final String ID = "scheduler";
    public static final Key<SchedulerSystem<?>.Instance> KEY = new Key<SchedulerSystem<?>.Instance>(ID, new TypeToken<SchedulerSystem<?>.Instance>() {});

    public static final long EXPIRE_THRESHOLD = 100;

    public interface GlobalScheduler<E extends ItemTreeEvent.Hold> {
        <N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance> int schedule(Y system, long delay, SystemTask<E, N, Y> task);
        void unschedule(int taskId);
        boolean run(E event, int id);

        static <E extends ItemTreeEvent.Hold> GlobalScheduler<E> create() {
            return new GlobalSchedulerImpl<>();
        }
    }

    private static final class GlobalSchedulerImpl<E extends ItemTreeEvent.Hold> implements GlobalScheduler<E> {
        private record Task<E extends ItemTreeEvent.Hold, N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance>(
                String[] path,
                String systemId,
                long runAt,
                SystemTask<E, N, Y> task
        ) {}

        private final Map<Integer, Task<E, ?, ?>> tasks = new HashMap<>();
        private int next;

        public int nextId() {
            return ++next;
        }

        @Override
        public <N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance> int schedule(Y system, long delay, SystemTask<E, N, Y> task) {
            Task<E, N, Y> inst = new Task<>(system.parent().path(), system.base().id(), java.lang.System.currentTimeMillis() + delay, task);
            int id = nextId();
            tasks.put(id, inst);
            return id;
        }

        @Override
        public void unschedule(int taskId) {
            tasks.remove(taskId);
        }

        @Override
        public boolean run(E event, int id) {
            Task<E, ?, ?> task = tasks.get(id);
            if (task == null)
                return true;
            long time = java.lang.System.currentTimeMillis();
            if (time < task.runAt)
                return false;
            if (time > task.runAt + EXPIRE_THRESHOLD)
                return true;
            return run0(task, event, id);
        }

        private <N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance> boolean run0(Task<E, N, Y> task, E event, int id) {
            @SuppressWarnings("unchecked")
            N root = (N) event.node();
            TaskContext<N, Y> ctx = new TaskContext<>(root);
            root.node(task.path).flatMap(node -> node.system(task.systemId)).ifPresent(sys -> {
                @SuppressWarnings("unchecked")
                Y ySys = (Y) sys;
                task.task.run(ySys, event, ctx);
            });
            return true;
        }
    }

    public record TaskContext<N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance>(N root) {
        public Optional<N> current(TreeNode original) {
            return root.node(original.path());
        }

        @SuppressWarnings("unchecked")
        public <T extends Y> Optional<T> current(T original) {
            return current(original.parent()).flatMap(n -> (Optional<T>) (n.system(original.base().id())));
        }
    }

    public interface SystemTask<E extends ItemTreeEvent.Hold, N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance> {
        void run(Y self, E event, TaskContext<N, Y> ctx);
    }

    protected final GlobalScheduler<E> scheduler;

    public SchedulerSystem(GlobalScheduler<E> scheduler, int listenerPriority) {
        super(listenerPriority);
        this.scheduler = scheduler;
    }

    public GlobalScheduler<E> scheduler() { return scheduler; }

    /**
     * See {@link SchedulerSystem}.
     */
    public abstract class Instance extends AbstractSystem.Instance {
        protected final List<Integer> tasks;
        protected long availableAt;

        public Instance(TreeNode parent, List<Integer> tasks, long availableAt) {
            super(parent);
            this.tasks = tasks;
            this.availableAt = availableAt;
        }

        @Override public abstract SchedulerSystem<E> base();

        public List<Integer> tasks() { return tasks; }
        public long availableAt() { return availableAt; }

        public boolean available() { return java.lang.System.currentTimeMillis() >= availableAt; }
        public void delay(long ms) { availableAt = java.lang.System.currentTimeMillis() + ms; }

        protected abstract Class<E> eventType();

        @Override
        public void build(StatLists stats) {
            parent.events().register(eventType(), this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.Unequip.class, this::event, listenerPriority);
        }

        public <N extends TreeNode.Scoped<N, ?, ?, ?, Y>, Y extends System.Instance> int schedule(Y system, long delay, SystemTask<E, N, Y> task) {
            int taskId = scheduler.schedule(system, delay, task);
            tasks.add(taskId);
            return taskId;
        }

        public void unschedule(int taskId) {
            scheduler.unschedule(taskId);
            tasks.remove((Object) taskId);
        }

        private void event(E event) {
            var iter = tasks.iterator();
            while (iter.hasNext()) {
                if (scheduler.run(event, iter.next())) {
                    iter.remove();
                    event.queueUpdate(ItemStack::hideUpdate);
                }
            }
        }

        private void event(ItemTreeEvent.Unequip event) {
            tasks.clear();
            event.queueUpdate();
        }
    }

    @Override public String id() { return ID; }
}
