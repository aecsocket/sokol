package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.tree.AbstractTreeNode;
import com.gitlab.aecsocket.sokol.core.tree.TreeEvent;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public class PaperTreeNode extends AbstractTreeNode<PaperTreeNode, PaperComponent, PaperSlot, PaperSystem, PaperSystem.Instance> {
    public static class Serializer implements TypeSerializer<PaperTreeNode> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        @Override
        public void serialize(Type type, @Nullable PaperTreeNode obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                String id = obj.value.id();
                ConfigurationNode children = node.node("children").set(obj.children);
                ConfigurationNode systems = node.node("systems").set(obj.systems);
                if (children.empty() && systems.empty())
                    node.set(id);
                else
                    node.node("id").set(id);
            }
        }

        private PaperTreeNode deserialize0(Type type, ConfigurationNode node) throws SerializationException {
            String id = (node.isMap() ? node.node("id") : node).getString();
            PaperComponent component = plugin.component(id);
            if (component == null)
                throw new SerializationException(node, type, "No component with ID [" + id + "]");

            PaperTreeNode tree = new PaperTreeNode(component);

            if (node.isMap()) {
                plugin.systemSerializer().base(tree);
                for (var entry : node.node("children").get(new TypeToken<Map<String, PaperTreeNode>>() {}, Collections.emptyMap()).entrySet()) {
                    tree.child(entry.getKey(), entry.getValue());
                }
                for (var entry : node.node("systems").get(new TypeToken<Map<String, PaperSystem.Instance>>() {}, Collections.emptyMap()).entrySet()) {
                    tree.system(entry.getValue());
                }
            }

            return tree;
        }

        @Override
        public PaperTreeNode deserialize(Type type, ConfigurationNode node) throws SerializationException {
            try {
                return deserialize0(type, node).build();
            } catch (IllegalArgumentException e) {
                throw new SerializationException(node, type, e);
            }
        }
    }

    public PaperTreeNode(PaperComponent value, EventDispatcher<TreeEvent> events, StatMap stats, String key, PaperTreeNode parent) {
        super(value, events, stats, key, parent);
    }

    public PaperTreeNode(PaperComponent value, EventDispatcher<TreeEvent> events, StatMap stats) {
        super(value, events, stats);
    }

    public PaperTreeNode(PaperComponent value, PaperTreeNode o) {
        super(value, o);
    }

    public PaperTreeNode(PaperComponent value) {
        super(value);
    }

    @Override protected PaperTreeNode self() { return this; }

    public @NotNull PaperTreeNode child(String key, PaperComponent component) {
        child(key, new PaperTreeNode(component, this));
        return this;
    }

    public @NotNull PaperTreeNode asRoot() {
        return value.asTree();
    }
}
