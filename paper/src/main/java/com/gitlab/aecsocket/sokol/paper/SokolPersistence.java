package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.ChatPosition;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Duration;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.paper.impl.PaperComponent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeatureInstance;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;

public final class SokolPersistence {
    private final SokolPlugin plugin;
    private final NamespacedKey keyTree;
    private final NamespacedKey keyId;
    private final NamespacedKey keySlots;
    private final NamespacedKey keyFeatures;
    private final DataType dataType;
    private long nextLog;

    SokolPersistence(SokolPlugin plugin) {
        this.plugin = plugin;
        keyTree = plugin.key("tree");
        keyId = plugin.key("id");
        keySlots = plugin.key("slots");
        keyFeatures = plugin.key("features");
        dataType = new DataType();
    }

    public boolean hasTree(PersistentDataContainer pdc) {
        return pdc.getKeys().contains(keyTree);
    }

    public PersistentDataContainer save(PersistentDataContainer pdc, PaperNode node) {
        pdc.set(keyTree, dataType, node);
        return pdc;
    }

    public PaperNode load(PersistentDataContainer pdc) throws IllegalArgumentException {
        PaperNode result = pdc.get(keyTree, dataType);
        if (result == null)
            throw new IllegalArgumentException("null");
        return result;
    }

    public Optional<PaperNode> safeLoad(PersistentDataContainer pdc) {
        if (hasTree(pdc)) {
            try {
                return Optional.of(load(pdc));
            } catch (IllegalArgumentException e) {
                if (plugin.setting(true, ConfigurationNode::getBoolean, "persistence", "log_errors")) {
                    if (System.currentTimeMillis() >= nextLog) {
                        nextLog = System.currentTimeMillis() + plugin.setting(Duration.duration(60 * 1000), (n, d) -> n.get(Duration.class, d), "persistence", "next_log").ms();
                        plugin.log(Logging.Level.WARNING, e, "Could not load tree from item's data container");
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<PaperNode> safeLoad(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return Optional.empty();
        return safeLoad(item.getItemMeta().getPersistentDataContainer());
    }

    private final class DataType implements PersistentDataType<PersistentDataContainer, PaperNode> {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public @NotNull Class<PaperNode> getComplexType() { return PaperNode.class; }

        @Override
        public @NotNull PersistentDataContainer toPrimitive(@NotNull PaperNode obj, @NotNull PersistentDataAdapterContext ctx) {
            PersistentDataContainer pdc = ctx.newPersistentDataContainer();
            pdc.set(keyId, PersistentDataType.STRING, obj.value().id());

            PersistentDataContainer pdcSlots = ctx.newPersistentDataContainer();
            for (var entry : obj.nodes().entrySet()) {
                pdcSlots.set(plugin.key(entry.getKey()), this, entry.getValue());
            }
            pdc.set(keySlots, PersistentDataType.TAG_CONTAINER, pdcSlots);

            PersistentDataContainer pdcFeatures = ctx.newPersistentDataContainer();
            for (var entry : obj.features().entrySet()) {
                PersistentDataContainer saved = ctx.newPersistentDataContainer();
                entry.getValue().save(saved);
                if (saved.getKeys().size() > 0)
                    pdcFeatures.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, saved);
            }
            pdc.set(keyFeatures, PersistentDataType.TAG_CONTAINER, pdcFeatures);

            return pdc;
        }

        @Override
        public @NotNull PaperNode fromPrimitive(@NotNull PersistentDataContainer pdc, @NotNull PersistentDataAdapterContext ctx) {
            String id = pdc.get(keyId, PersistentDataType.STRING);
            if (id == null)
                throw new IllegalArgumentException("Missing tag 'id'");
            PaperComponent value = plugin.components().get(id)
                    .orElseThrow(() -> new IllegalArgumentException("No component with ID '" + id + "'"));

            Map<String, PaperFeatureInstance> features = new HashMap<>();
            @SuppressWarnings("deprecation") // internal use
            PaperNode root = new PaperNode(value, null, features, null);
            PersistentDataContainer pdcSlots = pdc.get(keySlots, PersistentDataType.TAG_CONTAINER);
            if (pdcSlots != null) {
                for (var key : pdcSlots.getKeys()) {
                    Slot slot = value.slot(key.value())
                            .orElseThrow(() -> new IllegalArgumentException("No slot '" + key + "' exists on component '" + id + "'"));
                    //noinspection ConstantConditions - we know the key exists
                    PaperNode child = fromPrimitive(pdcSlots.get(key, PersistentDataType.TAG_CONTAINER), ctx);
                    try {
                        slot.compatibility(root, child);
                    } catch (IncompatibilityException e) {
                        throw new IllegalArgumentException("Incompatible node for slot '" + key + "'", e);
                    }
                    root.unsafeNode(key.value(), child);
                }
            }

            PersistentDataContainer pdcFeatures = pdc.get(keyFeatures, PersistentDataType.TAG_CONTAINER);
            if (pdcFeatures != null) {
                for (var key : pdcFeatures.getKeys()) {
                    String featureId = key.value();
                    PaperFeature<?> feature = value.feature(featureId)
                                    .orElseThrow(() -> new IllegalArgumentException("No feature with ID '" + featureId + "' on component '" + id + "'"));
                    //noinspection ConstantConditions - we know the key exists
                    features.put(key.value(), feature.load(root, pdcFeatures.get(key, PersistentDataType.TAG_CONTAINER)));
                }
            }
            root.fillDefaultFeatures();

            return root;
        }
    }
}
