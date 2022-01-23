package com.gitlab.aecsocket.paper;

import com.gitlab.aecsocket.minecommons.core.Duration;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.paper.impl.*;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SokolPersistence {
    private final SokolPlugin plugin;
    private final NamespacedKey keyTree;
    private final NamespacedKey keyId;
    private final NamespacedKey keySlots;
    private final NamespacedKey keyFeatureData;
    private final DataType dataType;
    private long nextLog;

    SokolPersistence(SokolPlugin plugin) {
        this.plugin = plugin;
        keyTree = plugin.key("tree");
        keyId = plugin.key("id");
        keySlots = plugin.key("slots");
        keyFeatureData = plugin.key("feature_data");
        dataType = new DataType();
    }

    public void save(PersistentDataContainer pdc, PaperBlueprint blueprint) {
        pdc.set(keyTree, dataType, blueprint);
    }

    public boolean hasTree(PersistentDataContainer pdc) {
        return pdc.getKeys().contains(keyTree);
    }

    public PaperBlueprint load(PersistentDataContainer pdc) throws PersistenceException {
        try {
            PaperBlueprint result = pdc.get(keyTree, dataType);
            if (result == null)
                throw new PersistenceException("null");
            return result;
        } catch (IllegalArgumentException e) {
            throw new PersistenceException(e);
        }
    }

    public Optional<PaperBlueprint> safeLoad(PersistentDataContainer pdc) {
        if (hasTree(pdc)) {
            try {
                return Optional.of(load(pdc));
            } catch (PersistenceException e) {
                if (plugin.setting(true, ConfigurationNode::getBoolean, "persistence", "log_errors")) {
                    if (System.currentTimeMillis() >= nextLog) {
                        nextLog = System.currentTimeMillis() + plugin.setting(Duration.duration(60 * 1000),
                                (n, d) -> n.get(Duration.class, d), "persistence", "log_interval").ms();
                        plugin.log(Logging.Level.WARNING, e, "Could not load tree from item's data container");
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<PaperBlueprint> safeLoad(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return Optional.empty();
        return safeLoad(item.getItemMeta().getPersistentDataContainer());
    }

    private final class DataType implements PersistentDataType<PersistentDataContainer, PaperBlueprint> {
        @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
        @Override public @NotNull Class<PaperBlueprint> getComplexType() { return PaperBlueprint.class; }

        @Override
        public @NotNull PersistentDataContainer toPrimitive(@NotNull PaperBlueprint obj, @NotNull PersistentDataAdapterContext ctx) {
            PersistentDataContainer pdc = ctx.newPersistentDataContainer();
            pdc.set(keyId, PersistentDataType.STRING, obj.value().id());

            PersistentDataContainer pdcSlots = ctx.newPersistentDataContainer();
            for (var entry : obj.children().entrySet()) {
                pdcSlots.set(plugin.key(entry.getKey()), this, entry.getValue());
            }
            pdc.set(keySlots, PersistentDataType.TAG_CONTAINER, pdcSlots);

            PersistentDataContainer pdcFeatureData = ctx.newPersistentDataContainer();
            for (var entry : obj.features().entrySet()) {
                PersistentDataContainer saved = ctx.newPersistentDataContainer();
                entry.getValue().save(saved, ctx);
                if (saved.getKeys().size() > 0)
                    pdcFeatureData.set(plugin.key(entry.getKey()), PersistentDataType.TAG_CONTAINER, saved);
            }
            pdc.set(keyFeatureData, PersistentDataType.TAG_CONTAINER, pdcFeatureData);

            return pdc;
        }

        @Override
        public @NotNull PaperBlueprint fromPrimitive(@NotNull PersistentDataContainer pdc, @NotNull PersistentDataAdapterContext ctx) {
            String id = pdc.get(keyId, PersistentDataType.STRING);
            if (id == null)
                throw new IllegalArgumentException("Missing tag 'id'");
            PaperComponent value = plugin.components().get(id)
                    .orElseThrow(() -> new IllegalArgumentException("No component with ID '" + id + "'"));

            Map<String, PaperFeatureData> featureData = new HashMap<>();
            PaperBlueprint root = new PaperBlueprint(plugin, value, featureData);

            PersistentDataContainer pdcSlots = pdc.get(keySlots, PersistentDataType.TAG_CONTAINER);
            if (pdcSlots != null) {
                for (var key : pdcSlots.getKeys()) {
                    String vKey = key.value();
                    PaperNodeSlot nodeSlot = value.slot(vKey)
                            .orElseThrow(() -> new IllegalArgumentException("No slot '" + vKey + "' exists on component '" + id + "'"));
                    //noinspection ConstantConditions - we know the key exists
                    PaperBlueprint child = fromPrimitive(pdcSlots.get(key, PersistentDataType.TAG_CONTAINER), ctx);
                    root.setUnsafe(vKey, child); // TODO make this a safe set?
                }
            }

            PersistentDataContainer pdcFeatureData = pdc.get(keyFeatureData, PersistentDataType.TAG_CONTAINER);
            if (pdcFeatureData != null) {
                for (var key : pdcFeatureData.getKeys()) {
                    String vKey = key.value();
                    PaperFeatureConfig featureConfig = value.feature(vKey)
                            .orElseThrow(() -> new IllegalArgumentException("No feature config with ID '" + vKey + "' exists on component '" + id + "'"));
                    //noinspection ConstantConditions - we know the key exists
                    PaperFeatureData loaded = featureConfig.load(pdcFeatureData.get(key, PersistentDataType.TAG_CONTAINER));
                    featureData.put(vKey, loaded);
                }
            }

            return root;
        }
    }
}
