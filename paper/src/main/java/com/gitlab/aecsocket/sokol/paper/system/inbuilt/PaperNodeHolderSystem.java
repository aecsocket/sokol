package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.gitlab.aecsocket.minecommons.core.Quantifier;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.NodeHolderSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class PaperNodeHolderSystem extends NodeHolderSystem<PaperTreeNode> implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.empty(ID);

    public final class Instance extends NodeHolderSystem<PaperTreeNode>.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent, List<Quantifier<PaperTreeNode>> held) {
            super(parent, held);
        }

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperNodeHolderSystem base() { return PaperNodeHolderSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        protected boolean equal(PaperTreeNode a, PaperTreeNode b) {
            // test if they serialize to the same value
            // this *works* but ehh
            ConfigurationNode cfg = platform.loaderBuilder().build().createNode();
            try {
                return cfg.copy().set(a)
                        .equals(cfg.copy().set(b));
            } catch (SerializationException e) {
                return false;
            }
        }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            List<PersistentDataContainer> cHeld = new ArrayList<>();
            for (var entry : held) {
                PersistentDataContainer dEntry = ctx.newPersistentDataContainer();
                PersistentDataContainer dNode = ctx.newPersistentDataContainer();
                platform.persistenceManager().save(dNode, entry.object());
                dEntry.set(platform.key("node"), PersistentDataType.TAG_CONTAINER, dNode);
                dEntry.set(platform.key("amount"), PersistentDataType.INTEGER, entry.amount());
                cHeld.add(dEntry);
            }
            data.set(platform.key("held"), PersistentDataType.TAG_CONTAINER_ARRAY, cHeld.toArray(new PersistentDataContainer[0]));
            return data;
        }

        @Override
        public void save(Type type, ConfigurationNode node) throws SerializationException {
            node.node("held").set(held);
        }
    }

    private final SokolPlugin platform;

    public PaperNodeHolderSystem(SokolPlugin platform, int listenerPriority, int capacity, boolean sizeAsDurability, @Nullable Rule rule) {
        super(listenerPriority, capacity, sizeAsDurability, rule);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        var cHeld = data.getOrDefault(platform.key("held"), PersistentDataType.TAG_CONTAINER_ARRAY, new PersistentDataContainer[0]);
        List<Quantifier<PaperTreeNode>> held = new LinkedList<>();
        for (var entry : cHeld) {
            var dNode = entry.get(platform.key("node"), PersistentDataType.TAG_CONTAINER);
            var dAmount = entry.get(platform.key("amount"), PersistentDataType.INTEGER);
            if (dNode == null || dAmount == null)
                continue;
            platform.persistenceManager().load(dNode).ifPresent(dsr ->
                    held.add(new Quantifier<>(dsr, dAmount)));
        }
        return new Instance(node, held);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform) {
        return cfg -> new PaperNodeHolderSystem(platform,
                cfg.node(keyListenerPriority).getInt(),
                cfg.node("capacity").getInt(-1),
                cfg.node("size_as_durability").getBoolean(false),
                null);
    }
}
