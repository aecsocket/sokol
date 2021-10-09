package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.AbstractComponent;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.ValidationException;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public final class PaperComponent extends AbstractComponent<PaperComponent, PaperSlot, PaperFeature<?>, PaperNode> {
    private final SokolPlugin platform;

    public PaperComponent(SokolPlugin platform, String id, Set<String> tags, Map<String, PaperSlot> slots, Map<String, PaperFeature<?>> featureTypes, StatIntermediate stats) {
        super(id, tags, slots, featureTypes, stats);
        this.platform = platform;
    }

    @Override
    public SokolPlugin platform() { return platform; }

    public static final class Serializer implements TypeSerializer<PaperComponent> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void serialize(Type type, @Nullable PaperComponent obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaperComponent deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Objects.requireNonNull(node.key()).toString();
            try {
                Keyed.validate(id);
            } catch (ValidationException e) {
                throw new SerializationException(node, type, "Invalid ID '" + id + "'", e);
            }

            Map<String, PaperFeature<?>> features = new HashMap<>();
            for (var entry : node.node("features").childrenMap().entrySet()) {
                String featureId = ""+entry.getKey();
                ConfigurationNode config = entry.getValue();
            }

            PaperComponent result = new PaperComponent(plugin,
                    id,
                    node.node("tags").get(new TypeToken<Set<String>>(){}, Collections.emptySet()),
                    node.node("slots").get(new TypeToken<Map<String, PaperSlot>>(){}, Collections.emptyMap()),
                    features,
                    node.node("stats").get(StatIntermediate.class, new StatIntermediate())
            );

            for (var entry : result.slots.entrySet()) {
                String key = entry.getKey();
                try {
                    Keyed.validate(key);
                } catch (ValidationException e) {
                    throw new SerializationException(node, type, "Invalid slot key '" + key + "'", e);
                }
                entry.getValue().parent(result, key);
            }

            return result;
        }
    }
}
