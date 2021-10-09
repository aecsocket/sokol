package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.AbstractComponent;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.ValidationException;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public final class PaperComponent extends AbstractComponent<PaperComponent, PaperSlot, PaperFeature<?>, PaperNode> {
    private final SokolPlugin platform;

    public PaperComponent(SokolPlugin platform, String id, Set<String> tags, Map<String, PaperSlot> slots, Map<String, PaperFeature<?>> featureTypes, StatIntermediate stats) {
        super(id, tags, slots, featureTypes, stats);
        this.platform = platform;
    }

    @Override
    public SokolPlugin platform() { return platform; }

    public Component slotName(String key, Locale locale) {
        return platform.lc().safe(locale, "slot." + key);
    }

    @Override
    public String toString() {
        return "PaperComponent:" + id + '{' +
                "tags=" + tags +
                ", slots=" + slots +
                ", featureTypes=" + featureTypes.keySet() +
                '}';
    }

    public static final class Serializer implements TypeSerializer<PaperComponent> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void serialize(Type type, @Nullable PaperComponent obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public PaperComponent deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Objects.requireNonNull(node.key()).toString();
            try {
                Keyed.validate(id);
            } catch (ValidationException e) {
                throw new SerializationException(node, type, "Invalid ID '" + id + "'", e);
            }

            Map<String, Stat<?>> statTypes = new HashMap<>();
            Map<String, Class<? extends Rule>> ruleTypes = new HashMap<>();

            Map<String, PaperFeature<?>> features = new HashMap<>();
            for (var entry : node.node("features").childrenMap().entrySet()) {
                String featureId = ""+entry.getKey();
                ConfigurationNode config = entry.getValue();
            }

            plugin.statMapSerializer().types(statTypes);
            plugin.ruleSerializer().types(ruleTypes);

            Map<String, PaperSlot> slots = new HashMap<>();
            for (var entry : node.node("slots").childrenMap().entrySet()) {
                String key = entry.getKey()+"";
                ConfigurationNode value = entry.getValue();
                try {
                    Keyed.validate(key);
                } catch (ValidationException e) {
                    throw new SerializationException(value, type, "Invalid slot key '" + key + "'", e);
                }
                slots.put(key, require(value, PaperSlot.class));
            }

            // TODO load the feature configs

            PaperComponent result = new PaperComponent(plugin,
                    id,
                    node.node("tags").get(new TypeToken<Set<String>>(){}, Collections.emptySet()),
                    slots,
                    features,
                    node.node("stats").get(StatIntermediate.class, new StatIntermediate())
            );

            plugin.statMapSerializer().types(null);
            plugin.ruleSerializer().types(null);

            for (var entry : result.slots.entrySet())
                entry.getValue().parent(result, entry.getKey());

            return result;
        }
    }
}
