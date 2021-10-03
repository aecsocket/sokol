package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.AbstractComponent;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.feature.LoadProvider;
import com.gitlab.aecsocket.sokol.paper.system.PaperFeature;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Paper platform-specific concrete implementation of a component.
 */
public final class PaperComponent extends AbstractComponent<PaperComponent, PaperSlot, PaperFeature> {
    /**
     * Type serializer for a {@link PaperComponent}.
     */
    public static final class Serializer implements TypeSerializer<PaperComponent> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        @Override
        public void serialize(Type type, @Nullable PaperComponent obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.node("slots").set(obj.slots);
                node.node("systems").set(obj.baseSystems);
                node.node("tags").set(obj.tags);
            }
        }

        @Override
        public PaperComponent deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Objects.toString(node.key());
            if (!Keyed.validKey(id))
                throw new SerializationException(node, type, "Invalid ID [" + id + "], must match " + Keyed.VALID_KEY);

            List<LoadProvider> loadProviders = new ArrayList<>();
            Map<String, PaperFeature> systems = new HashMap<>();
            Map<PaperFeature, ConfigurationNode> systemConfigs = new HashMap<>();
            for (var entry : node.node("systems").childrenMap().entrySet()) {
                String systemId = ""+entry.getKey();
                ConfigurationNode child = entry.getValue();
                PaperFeature.KeyedType systemType = plugin.systemTypes().get(systemId);
                if (systemType == null)
                    throw new SerializationException(node, type, "Could not find system [" + systemId + "]");
                try {
                    PaperFeature system = systemType.createSystem(child);
                    if (system == null)
                        throw new SerializationException(child, type, "Null system");
                    loadProviders.add(system);
                    systems.put(systemId, system);
                    systemConfigs.put(system, entry.getValue());
                } catch (SerializationException | RuntimeException e) {
                    throw new SerializationException(child, type, "Could not create system [" + systemId + "]", e);
                }
            }
            for (var child : node.node("load_systems").childrenList()) {
                String systemId = Serializers.require(child, String.class);
                PaperFeature.KeyedType systemType = plugin.systemTypes().get(systemId);
                if (systemType == null)
                    throw new SerializationException(child, type, "Could not find system [" + systemId + "]");
                LoadProvider provider = systemType.createLoadProvider();
                if (provider == null)
                    throw new SerializationException(child, type, "Null provider for [" + systemId + "]");
                loadProviders.add(provider);
            }

            StatTypes statTypes = new StatTypes();
            Map<String, Class<? extends Rule>> ruleTypes = new HashMap<>(Rule.BASE_RULE_TYPES);
            for (var provider : loadProviders) {
                statTypes.putAll(provider.statTypes());
                String pfx = provider.id() + ":";
                for (var ruleType : provider.ruleTypes().entrySet())
                    ruleTypes.put(pfx + ruleType.getKey(), ruleType.getValue());
            }
            plugin.statMapSerializer().types(statTypes);
            plugin.ruleSerializer().types(ruleTypes);

            for (var entry : systemConfigs.entrySet()) {
                entry.getKey().loadSelf(entry.getValue());
            }

            PaperComponent result = new PaperComponent(plugin,
                    Objects.toString(node.key()),
                    node.node("slots").get(new TypeToken<Map<String, PaperSlot>>() {}, Collections.emptyMap()),
                    systems,
                    node.node("tags").get(new TypeToken<Set<String>>() {}, Collections.emptySet()),
                    node.node("stats").get(StatLists.class, new StatLists())
            );

            // Reset the types, so that future deserialization calls *have* to provide types.
            plugin.statMapSerializer().types(null);
            plugin.ruleSerializer().types(null);

            for (var entry : result.slots.entrySet()) {
                String key = entry.getKey();
                if (!Keyed.validKey(key))
                    throw new SerializationException(node, type, "Invalid slot key [" + key + "], must be " + Keyed.VALID_KEY);
                entry.getValue().parent(entry.getKey(), result);
            }
            return result;
        }
    }

    private final SokolPlugin platform;

    public PaperComponent(SokolPlugin platform, String id, Map<String, PaperSlot> slots, Map<String, PaperFeature> baseSystems, Collection<String> tags, StatLists stats) {
        super(id, slots, baseSystems, tags, stats);
        this.platform = platform;
    }

    public PaperComponent(SokolPlugin platform, Scoped<PaperComponent, PaperSlot, PaperFeature> o) {
        super(o);
        this.platform = platform;
    }

    public PaperComponent(PaperComponent o) {
        this(o.platform, o);
    }

    @Override public SokolPlatform platform() { return platform; }
    @Override public PaperComponent self() { return this; }

    public PaperTreeNode asTree() {
        return new PaperTreeNode(this).build();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaperComponent that = (PaperComponent) o;
        return platform.equals(that.platform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), platform);
    }

    @Override
    public String toString() {
        return "PaperComponent{" +
                "id='" + id + '\'' +
                ", slots=" + slots +
                ", baseSystems=" + baseSystems.keySet() +
                ", tags=" + tags +
                ", stats=" + stats +
                '}';
    }
}
