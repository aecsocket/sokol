package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.event.ItemEvent;
import com.github.aecsocket.sokol.core.impl.AbstractFeatureInstance;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.github.aecsocket.sokol.core.world.ItemStack;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

public abstract class TaskSchedulerImpl<
    F extends TaskSchedulerImpl<F, P, D, I, N, S>,
    P extends TaskSchedulerImpl<F, P, D, I, N, S>.Profile,
    D extends TaskSchedulerImpl<F, P, D, I, N, S>.Profile.Data,
    I extends TaskSchedulerImpl<F, P, D, I, N, S>.Profile.Instance,
    N extends TreeNode.Scoped<N, ?, ?, ?, S>,
    S extends ItemStack.Scoped<S, ?>
> implements Feature<P> {
    public static final String
        ID = "task_scheduler",
        TASKS = "tasks",
        READY_AT = "ready_at";

    public interface Manager {
        <N extends TreeNode.Scoped<N, ?, ?, ?, ?>, F extends FeatureInstance<?, ?, N>> int schedule(F feature, long delay, TaskScheduler.Task<N, F> task);

        void unschedule(int taskId);

        boolean handle(TreeNode node, int taskId);
    }

    public static ManagerImpl createManager() {
        return new ManagerImpl();
    }

    private static final class ManagerImpl implements Manager {
        private static final long EXPIRE = 100;

        private record TaskData<
            N extends TreeNode.Scoped<N, ?, ?, ?, ?>,
            F extends FeatureInstance<?, ?, N>
        >(NodePath path, String feature, long at, TaskScheduler.Task<N, F> task) {}

        private final Map<Integer, TaskData<?, ?>> tasks = new HashMap<>();
        private int nextId;

        public int nextId() { return ++nextId; }

        @Override
        public <N extends TreeNode.Scoped<N, ?, ?, ?, ?>, F extends FeatureInstance<?, ?, N>> int schedule(F feature, long delay, TaskScheduler.Task<N, F> task) {
            TaskData<N, F> data = new TaskData<>(feature.parent().path(), feature.profile().type().id(), System.currentTimeMillis() + delay, task);
            int id = nextId();
            tasks.put(id, data);
            return id;
        }

        @Override
        public void unschedule(int taskId) {
            tasks.remove(taskId);
        }

        // returns `true` if the task id should be removed
        @Override
        public boolean handle(TreeNode node, int taskId) {
            TaskData<?, ?> task = tasks.get(taskId);
            if (task == null)
                return true;
            long time = System.currentTimeMillis();
            if (time < task.at)
                return false;
            if (time > task.at + EXPIRE)
                return true;
            run0(node, task);
            return true;
        }

        private <N extends TreeNode.Scoped<N, ?, ?, ?, ?>, F extends FeatureInstance<?, ?, N>> void run0(TreeNode node, TaskData<N, F> task) {
            @SuppressWarnings("unchecked")
            N scoped = (N) node;
            node.get(task.path).flatMap(child -> child.feature(task.feature)).ifPresent(feature -> {
                @SuppressWarnings("unchecked")
                F casted = (F) feature;
                task.task.run(casted, TaskScheduler.createContext(scoped));
            });
        }
    }

    protected abstract F self();
    protected abstract SokolPlatform platform();
    protected abstract Manager taskManager();

    @Override public StatTypes statTypes() { return StatTypes.empty(); }
    @Override public RuleTypes ruleTypes() { return RuleTypes.empty(); }
    @Override public final String id() { return ID; }

    public abstract class Profile implements FeatureProfile<F, D> {
        protected final int listenerPriority;

        public Profile(int listenerPriority) {
            this.listenerPriority = listenerPriority;
        }

        public int listenerPriority() { return listenerPriority; }

        protected abstract P self();
        @Override public F type() { return TaskSchedulerImpl.this.self(); }

        @Override public void validate(SokolComponent parent) throws FeatureValidationException {}

        public abstract class Data implements FeatureData<P, I, N> {
            protected final List<Integer> tasks;
            protected final long readyAt;

            public Data(List<Integer> tasks, long readyAt) {
                this.tasks = tasks;
                this.readyAt = readyAt;
            }

            public List<Integer> tasks() { return tasks; }

            public long readyAt() { return readyAt; }
            public boolean ready() { return System.currentTimeMillis() >= readyAt; }

            @Override public P profile() { return Profile.this.self(); }

            @Override
            public void save(ConfigurationNode node) throws SerializationException {
                node.node(TASKS).set(tasks);
                node.node(READY_AT).set(readyAt);
            }
        }

        public abstract class Instance extends AbstractFeatureInstance<P, D, N> implements TaskScheduler<N> {
            protected final List<Integer> tasks;
            protected long readyAt;

            public Instance(List<Integer> tasks, long readyAt) {
                this.tasks = tasks;
                this.readyAt = readyAt;
            }

            public List<Integer> tasks() { return tasks; }

            @Override public long readyAt() { return readyAt; }
            @Override public boolean ready() { return readyAt >= System.currentTimeMillis(); }
            @Override public void delay(long ms) { readyAt = System.currentTimeMillis() + ms; }

            @Override public P profile() { return self(); }

            @Override
            public void build(Tree<N> tree, N parent, StatIntermediate stats) {
                super.build(tree, parent, stats);
                tree.events().register(new TypeToken<ItemEvent.Hold<N>>() {}, this::onEvent, listenerPriority);
            }

            @Override
            public <G extends FeatureInstance<?, ?, N>> int schedule(G feature, long delay, Task<N, G> task) {
                return taskManager().schedule(feature, delay, task);
            }

            @Override
            public void unschedule(int taskId) {
                taskManager().unschedule(taskId);
            }

            protected void onEvent(ItemEvent.Hold<N> event) {
                var iter = tasks.iterator();
                while (iter.hasNext()) {
                    if (taskManager().handle(parent, iter.next())) {
                        iter.remove();
                        // update item
                    }
                }
                event.updateItem(map -> is -> map.apply(is).);
            }
        }
    }
}
