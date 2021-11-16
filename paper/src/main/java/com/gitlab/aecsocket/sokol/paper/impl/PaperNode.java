package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.TreeData;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.UserEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import io.leangen.geantyref.TypeToken;
import org.bukkit.inventory.ItemStack;
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

public final class PaperNode extends AbstractNode<PaperNode, PaperComponent, PaperFeatureInstance> {
    @Deprecated
    public PaperNode(PaperComponent value, @Nullable NodeKey<PaperNode> key, Map<String, PaperFeatureInstance> features, TreeData.@Nullable Scoped<PaperNode> treeData) {
        super(value, key, features, treeData);
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

    private PaperItem createItem0(Locale locale) throws ItemCreationException {
        PaperItem item;
        try {
            ItemStack stack = treeData.stats().require(PaperComponent.STAT_ITEM)
                    .buildStack();
            stack.editMeta(meta -> {
                value.platform().persistence().save(meta.getPersistentDataContainer(), this);
                meta.displayName(Components.BLANK.append(value.render(locale, value.platform().lc())));
                value.renderDescription(locale, value.platform().lc()).ifPresent(meta::lore);
            });
            item = new PaperItem(stack);
        } catch (IllegalStateException e) {
            throw new ItemCreationException(e);
        }
        return item;
    }

    @Override
    public PaperItem createItem(ItemUser user) throws ItemCreationException {
        // Required to build the tree data, to get the STAT_ITEM stat
        initialize(user);
        PaperItem item = createItem0(user.locale());
        call(new Events.CreateItemUser(this, user, item));
        return item;
    }

    @Override
    public PaperItem createItem(Locale locale) throws ItemCreationException {
        // Required to build the tree data, to get the STAT_ITEM stat
        initialize(locale);
        PaperItem item = createItem0(locale);
        call(new Events.CreateItemLocalized(this, locale, item));
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
            PaperNode root = new PaperNode(value, null, features, null);
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

                root.fillDefaultFeatures();
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

        public interface CreateItem extends CreateItemEvent<PaperNode> {
            @Override PaperItem item();
        }

        public record CreateItemLocalized(
                PaperNode node,
                Locale locale,
                PaperItem item
        ) implements CreateItem {}

        public record CreateItemUser(
                PaperNode node,
                ItemUser user,
                PaperItem item
        ) implements CreateItem, UserEvent<PaperNode> {}
    }
}
