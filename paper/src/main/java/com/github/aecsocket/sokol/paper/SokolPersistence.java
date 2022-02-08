package com.github.aecsocket.sokol.paper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public final class SokolPersistence {
    private final SokolPlugin plugin;
    private final DataType dataType;
    private final NamespacedKey keyTree;
    private final NamespacedKey keyId;
    private final NamespacedKey keySlots;
    private final NamespacedKey keyFeatures;

    SokolPersistence(SokolPlugin plugin) {
        this.plugin = plugin;
        dataType = new DataType();
        keyTree = plugin.key("tree");
        keyId = plugin.key("id");
        keySlots = plugin.key("slots");
        keyFeatures = plugin.key("features");
    }

    public PaperBlueprintNode loadRaw(PersistentDataContainer pdc) throws IllegalArgumentException {
        return pdc.get(keyTree, dataType);
    }

    public Optional<PaperBlueprintNode> load(PersistentDataContainer pdc) {
        if (!pdc.has(keyTree))
            return Optional.empty();
        try {
            return Optional.of(loadRaw(pdc));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<PaperBlueprintNode> load(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return Optional.empty();
        return load(item.getItemMeta().getPersistentDataContainer());
    }

    private final class DataType implements PersistentDataType<PersistentDataContainer, PaperBlueprintNode> {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public Class<PaperBlueprintNode> getComplexType() { return PaperBlueprintNode.class; }

        @Override
        public PersistentDataContainer toPrimitive(PaperBlueprintNode node, PersistentDataAdapterContext ctx) {
            PersistentDataContainer pdc = ctx.newPersistentDataContainer();
            pdc.set(keyId, PersistentDataType.STRING, node.value().id());

            PersistentDataContainer pdcSlots = ctx.newPersistentDataContainer();
            for (var entry : node.children().entrySet()) {
                pdcSlots.set(plugin.key(entry.getKey()), this, entry.getValue());
            }
            pdc.set(keySlots, PersistentDataType.TAG_CONTAINER, pdcSlots);

            PersistentDataContainer pdcFeatures = ctx.newPersistentDataContainer();
            for (var entry : node.featureData().entrySet()) {
                PersistentDataContainer saved = ctx.newPersistentDataContainer();
                entry.getValue().save(saved, ctx);
                if (!saved.getKeys().isEmpty())
                    pdcFeatures.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, saved);
            }
            pdc.set(keyFeatures, PersistentDataType.TAG_CONTAINER, pdcFeatures);

            return pdc;
        }

        @Override
        public PaperBlueprintNode fromPrimitive(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) {
            String id = pdc.get(keyId, PersistentDataType.STRING);
            if (id == null)
                throw new IllegalArgumentException("Missing tag `id`");
            PaperComponent value = plugin.components().get(id)
                .orElseThrow(() -> new IllegalArgumentException("No component with ID `" + id + "`"));

            Map<String, PaperFeatureData> features = new HashMap<>();
            PaperBlueprintNode root = new PaperBlueprintNode(value, features);

            PersistentDataContainer pdcSlots = pdc.get(keySlots, PersistentDataType.TAG_CONTAINER);
            if (pdcSlots != null) {
                for (var key : pdcSlots.getKeys()) {
                    String vKey = key.value();
                    if (value.slot(vKey).isEmpty())
                        throw new IllegalArgumentException("No slot `" + vKey + "` exists on component `" + id + "`");
                    //noinspection ConstantConditions - we know the key exists
                    PaperBlueprintNode child = fromPrimitive(pdcSlots.get(key, PersistentDataType.TAG_CONTAINER), ctx);
                    root.setUnsafe(vKey, child);
                }
            }

            PersistentDataContainer pdcFeatures = pdc.get(keyFeatures, PersistentDataType.TAG_CONTAINER);
            if (pdcFeatures != null) {
                for (var key : pdcFeatures.getKeys()) {
                    String vKey = key.value();
                    PaperFeatureProfile profile = value.feature(vKey)
                        .orElseThrow(() -> new IllegalArgumentException("No feature profile with ID `" + vKey + "` exists on component `" + id + "`"));
                    //noinspection ConstantConditions - we know the key exists
                    PaperFeatureData feature = profile.load(pdcFeatures.get(key, PersistentDataType.TAG_CONTAINER));
                    features.put(vKey, feature);
                }
            }

            return root;
        }
    }
}
