package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperEvent;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

public final class PaperSchedulerSystem extends SchedulerSystem<PaperEvent.Hold> implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    private static final String keyTasks = "tasks";
    private static final String keyAvailableAt = "available_at";

    public final class Instance extends SchedulerSystem<PaperEvent.Hold>.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent, List<Integer> tasks, long availableAt) {
            super(parent, tasks, availableAt);
        }

        @Override public PaperSchedulerSystem base() { return PaperSchedulerSystem.this; }
        @Override public SokolPlugin platform() { return platform; }
        @Override protected Class<PaperEvent.Hold> eventType() { return PaperEvent.Hold.class; }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            int[] iTasks = new int[tasks.size()];
            for (int i = 0; i < tasks.size(); i++)
                iTasks[i] = tasks.get(i);
            data.set(platform.key(keyTasks), PersistentDataType.INTEGER_ARRAY, iTasks);
            data.set(platform.key(keyAvailableAt), PersistentDataType.LONG, availableAt);
            return data;
        }
    }

    private final SokolPlugin platform;

    public PaperSchedulerSystem(SokolPlugin platform, int listenerPriority) {
        super(platform.systemScheduler(), listenerPriority);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node, new ArrayList<>(), 0);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        int[] iTasks = data.getOrDefault(platform.key(keyTasks), PersistentDataType.INTEGER_ARRAY, new int[0]);
        List<Integer> tasks = new ArrayList<>(iTasks.length);
        for (int v : iTasks)
            tasks.add(v);
        return new Instance(node,
                tasks,
                data.getOrDefault(platform.key(keyAvailableAt), PersistentDataType.LONG, 0L));
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node,
                cfg.node("tasks").getList(Integer.class, new ArrayList<>()),
                cfg.node("available_at").getLong());
    }

    public static Type type(SokolPlugin platform) {
        return cfg -> new PaperSchedulerSystem(platform,
                cfg.node(keyListenerPriority).getInt());
    }
}
