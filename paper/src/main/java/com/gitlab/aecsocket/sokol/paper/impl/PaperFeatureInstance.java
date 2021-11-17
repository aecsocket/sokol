package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.FeatureInstance;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;

public interface PaperFeatureInstance extends FeatureInstance<PaperNode> {
    @Override PaperFeatureInstance copy(PaperNode parent);

    void save(Type type, ConfigurationNode node) throws SerializationException;
    void save(PersistentDataContainer pdc) throws IllegalArgumentException;

    final class Serializer implements TypeSerializer<PaperFeatureInstance> {
        private final SokolPlugin plugin;
        private @Nullable PaperNode base;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        public @Nullable PaperNode base() { return base; }
        public void base(@Nullable PaperNode base) { this.base = base; }

        @Override
        public void serialize(Type type, @Nullable PaperFeatureInstance obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                obj.save(type, node);
            }
        }

        @Override
        public PaperFeatureInstance deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (base == null)
                throw new SerializationException(node, type, "No base node provided to deserialize feature instances");

            String id = Objects.requireNonNull(node.key()).toString();
            PaperFeature<?> feature = base.value().feature(id)
                    .orElseThrow(() -> new SerializationException(node, type, "No feature with ID '" + id + "' on component '" + base.value().id() + "'"));
            return feature.load(base, type, node);
        }
    }
}
