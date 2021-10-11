package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.TreeData;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public class PaperNode extends AbstractNode<PaperNode, PaperComponent, PaperFeatureInstance> {
    public PaperNode(PaperComponent value, @Nullable NodeKey<PaperNode> key, Map<String, ? extends PaperFeatureInstance> features, TreeData.@Nullable Scoped<PaperNode> treeData) {
        super(value, key, features, treeData);
    }

    public PaperNode(PaperComponent value, @Nullable NodeKey<PaperNode> key, Map<String, ? extends PaperFeatureInstance> features) {
        super(value, key, features);
    }

    public PaperNode(PaperComponent value, @Nullable NodeKey<PaperNode> key) {
        super(value, key);
    }

    public PaperNode(PaperComponent value, @Nullable PaperNode parent, @Nullable String key) {
        super(value, parent, key);
    }

    public PaperNode(PaperComponent value) {
        super(value);
    }

    public PaperNode(PaperNode o) {
        super(o);
    }

    @Override
    protected PaperFeatureInstance copyFeature(PaperFeatureInstance val) {
        return val.copy();
    }

    @Override public PaperNode self() { return this; }

    @Override
    public PaperItem createItem(Locale locale) throws ItemCreationException {
        /*
        TODO this is stupid. so when we want to make an item out of an abstract node,
        we have to give it a treeData by building it... but to build it we need to
        pass an event... which we don't have since that would be the Events.CreateItem
        we create down below...????????
         */
        if (treeData == null)
            throw new ItemCreationException("Trying to create item with no tree data");
        PaperItem item = treeData.stats().require(PaperComponent.STAT_ITEM)
                .buildWrapper()
                .name(value.render(locale, value.platform().lc()));
        treeData.events().call(new Events.CreateItem(this, locale, item));
        return item;
    }

    @Override
    public PaperNode copy() {
        return new PaperNode(this);
    }

    public static final class Serializer implements TypeSerializer<PaperNode> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void serialize(Type type, @Nullable PaperNode obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                String id = obj.value.id();
                ConfigurationNode nodes = node.node("nodes").set(obj.nodes);
                ConfigurationNode features = node.node("features");
                for (var entry : obj.features.entrySet()) {
                    features.node(entry.getKey()).set(PaperFeatureInstance.class, entry.getValue());
                }

                if (nodes.empty() && features.empty())
                    node.set(id);
                else
                    node.node("id").set(id);
            }
        }

        private PaperNode deserialize0(Type type, ConfigurationNode node) throws SerializationException {
            String id = require(node.isMap() ? node.node("id") : node, String.class);
            PaperComponent value = plugin.components().get(id)
                    .orElseThrow(() -> new SerializationException(node, type, "No component with ID '" + id + "'"));

            Map<String, PaperFeatureInstance> features = new HashMap<>();
            PaperNode root = new PaperNode(value, null, features);
            if (node.isMap()) {
                for (var entry : node.node("nodes").childrenMap().entrySet()) {
                    String key = entry.getKey()+"";
                    ConfigurationNode childConfig = entry.getValue();
                    Slot slot = value.slot(key)
                            .orElseThrow(() -> new SerializationException(childConfig, type, "No slot '" + key + "' exists on component '" + value.id() + "'"));
                    PaperNode child = deserialize(type, childConfig);
                    try {
                        slot.compatibility(root, child);
                    } catch (IncompatibilityException e) {
                        throw new SerializationException(childConfig, type, "Incompatible node for slot '" + key + "'", e);
                    }
                    root.unsafeNode(key, child);
                }
                plugin.featureSerializer().base(root);
                features.putAll(node.node("features").get(new TypeToken<Map<String, PaperFeatureInstance>>(){}, Collections.emptyMap()));
                plugin.featureSerializer().base(null);
            }
            return root;
        }

        @Override
        public PaperNode deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return deserialize0(type, node);
        }
    }

    public static final class Events {
        private Events() {}

        public record CreateItem(
                PaperNode node,
                Locale locale,
                PaperItem item
        ) implements NodeEvent<PaperNode> {}
    }
}
