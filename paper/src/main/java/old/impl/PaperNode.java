package old.impl;

import com.github.aecsocket.sokol.core.Slot;
import com.github.aecsocket.sokol.core.Tree;
import com.github.aecsocket.sokol.core.event.CreateItemEvent;
import com.github.aecsocket.sokol.core.impl.AbstractNode;
import com.github.aecsocket.sokol.core.node.IncompatibilityException;
import com.github.aecsocket.sokol.core.node.ItemCreationException;
import com.github.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.minecommons.core.Components;

import old.SokolPlugin;
import old.wrapper.PaperItem;
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

public final class PaperNode extends AbstractNode<PaperNode, PaperItem, PaperComponent, PaperFeatureInstance> {
    @Deprecated
    public PaperNode(PaperComponent value, @Nullable NodeKey<PaperNode> key, Map<String, PaperFeatureInstance> features) {
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
        return val.copy(this);
    }

    @Override public PaperNode self() { return this; }

    private PaperItem createItem0(Locale locale, Tree<PaperNode> treeCtx) throws ItemCreationException {
        PaperItem item;
        try {
            ItemStack stack = treeCtx.stats().require(PaperComponent.STAT_ITEM)
                    .buildStack();
            stack.editMeta(meta -> {
                value.platform().persistence().save(meta.getPersistentDataContainer(), this);
                meta.displayName(Components.BLANK.append(value.render(locale, value.platform().lc())));
            });
            item = value.platform().wrap(stack);
        } catch (IllegalStateException e) {
            throw new ItemCreationException(e);
        }
        return item;
    }

    @Override
    public PaperItem createItem(ItemUser user) throws ItemCreationException {
        var tree = build(user);
        PaperItem item = createItem0(user.locale(), tree);
        new Events.CreateItemUser(tree, user, item).call();
        return item;
    }

    @Override
    public PaperItem createItem(Locale locale) throws ItemCreationException {
        var tree = build(locale);
        PaperItem item = createItem0(locale, tree);
        new Events.CreateItemLocalized(tree, locale, item).call();
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
                ConfigurationNode nodes = node.node("nodes").set(obj.children);
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
            Locale locale = plugin.defaultLocale();
            var tree = root.build(locale);
            if (node.isMap()) {
                for (var entry : node.node("nodes").childrenMap().entrySet()) {
                    String key = entry.getKey()+"";
                    ConfigurationNode childConfig = entry.getValue();
                    Slot slot = value.slot(key)
                            .orElseThrow(() -> new SerializationException(childConfig, type, "No slot '" + key + "' exists on component '" + value.id() + "'"));
                    PaperNode child = deserialize(type, childConfig);
                    try {
                        slot.compatibility(tree, child.build(locale));
                    } catch (IncompatibilityException e) {
                        throw new SerializationException(childConfig, type, "Incompatible node for slot '" + key + "'", e);
                    }
                    root.forceNode(key, child);
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

        public interface CreateItem extends CreateItemEvent<PaperNode, PaperItem> {}

        public record CreateItemLocalized(
                Tree<PaperNode> tree,
                Locale locale,
                PaperItem item
        ) implements CreateItem {}

        public record CreateItemUser(
                Tree<PaperNode> tree,
                ItemUser user,
                PaperItem item
        ) implements CreateItem, UserEvent<PaperNode> {}
    }
}
