package com.github.aecsocket.sokol.paper.feature;

import com.github.aecsocket.sokol.core.feature.TaskSchedulerImpl;
import com.github.aecsocket.sokol.paper.*;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PaperTaskScheduler extends TaskSchedulerImpl<
    PaperTaskScheduler, PaperTaskScheduler.Profile, PaperTaskScheduler.Profile.Data, PaperTaskScheduler.Profile.Instance, PaperTreeNode, PaperItemStack
> implements PaperFeature<PaperTaskScheduler.Profile> {
    public static final String
        LISTENER_PRIORITY = "listener_priority",
        TASKS = "tasks",
        READY_AT = "ready_at";

    private final SokolPlugin platform;

    public PaperTaskScheduler(SokolPlugin platform) {
        this.platform = platform;
    }

    @Override protected PaperTaskScheduler self() { return this; }
    @Override public SokolPlugin platform() { return platform; }

    @Override protected Manager taskManager() { return platform.taskManager(); }

    @Override
    public Profile setUp(ConfigurationNode node) throws SerializationException {
        return new Profile(
            node.node(LISTENER_PRIORITY).getInt()
        );
    }

    public final class Profile extends TaskSchedulerImpl<
        PaperTaskScheduler, PaperTaskScheduler.Profile, PaperTaskScheduler.Profile.Data, PaperTaskScheduler.Profile.Instance, PaperTreeNode, PaperItemStack
    >.Profile implements PaperFeatureProfile<PaperTaskScheduler, Profile.Data> {
        @Override protected Profile self() { return this; }

        public Profile(int listenerPriority) {
            super(listenerPriority);
        }

        @Override
        public Data setUp() {
            return new Data(new ArrayList<>(), 0);
        }

        @Override
        public Data load(ConfigurationNode node) throws SerializationException {
            return new Data(
                node.node(TASKS).getList(Integer.class, Collections.emptyList()),
                node.node(READY_AT).getLong()
            );
        }

        @Override
        public Data load(PersistentDataContainer pdc) {
            return new Data(
                Arrays.stream(pdc.getOrDefault(platform.key(TASKS), PersistentDataType.INTEGER_ARRAY, new int[0])).boxed().toList(),
                pdc.getOrDefault(platform.key(READY_AT), PersistentDataType.LONG, 0L)
            );
        }

        public final class Data extends TaskSchedulerImpl<
                PaperTaskScheduler, PaperTaskScheduler.Profile, PaperTaskScheduler.Profile.Data, PaperTaskScheduler.Profile.Instance, PaperTreeNode, PaperItemStack
        >.Profile.Data implements PaperFeatureData<Profile, Instance> {
            public Data(List<Integer> tasks, long readyAt) {
                super(tasks, readyAt);
            }

            @Override
            public Instance asInstance(PaperTreeNode node) {
                return new Instance(tasks, readyAt);
            }

            @Override
            public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) {
                pdc.set(platform.key(TASKS), PersistentDataType.INTEGER_ARRAY, tasks.stream().mapToInt(Integer::intValue).toArray());
                pdc.set(platform.key(READY_AT), PersistentDataType.LONG, readyAt);
            }
        }

        public final class Instance extends TaskSchedulerImpl<
                PaperTaskScheduler, PaperTaskScheduler.Profile, PaperTaskScheduler.Profile.Data, PaperTaskScheduler.Profile.Instance, PaperTreeNode, PaperItemStack
        >.Profile.Instance implements PaperFeatureInstance<Profile, Data> {
            public Instance(List<Integer> tasks, long readyAt) {
                super(tasks, readyAt);
            }

            @Override
            public Data asData() {
                return new Data(tasks, readyAt);
            }

            @Override
            public Instance copy() {
                return new Instance(new ArrayList<>(tasks), readyAt);
            }
        }
    }
}
