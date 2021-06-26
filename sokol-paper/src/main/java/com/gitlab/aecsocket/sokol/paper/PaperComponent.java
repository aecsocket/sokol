package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.AbstractComponent;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Paper platform-specific concrete implementation of a component.
 */
public final class PaperComponent extends AbstractComponent<PaperComponent, PaperSlot, PaperSystem> {
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
                throw new SerializationException(node, type, "Invalid ID [" + id + "], must be " + Keyed.VALID_KEY);

            Map<String, PaperSystem> systems = new HashMap<>();
            for (var entry : node.node("systems").childrenMap().entrySet()) {
                String systemId = (String) entry.getKey();
                PaperSystem.KeyedType systemType = plugin.systemTypes().get(systemId);
                if (systemType == null)
                    throw new SerializationException(node, type, "Could not find system [" + systemId + "]");
                try {
                    systems.put(systemId, systemType.create(plugin, entry.getValue()));
                } catch (SerializationException e) {
                    throw new SerializationException(node, type, "Could not create system [" + systemId + "]", e);
                }
            }

            Map<String, Stat<?>> statTypes = new HashMap<>();
            Map<String, Class<? extends Rule>> ruleTypes = new HashMap<>(Rule.BASE_RULE_TYPES);
            for (PaperSystem system : systems.values()) {
                statTypes.putAll(system.statTypes());
                ruleTypes.putAll(system.ruleTypes());
            }
            plugin.statMapSerializer().types(statTypes);
            plugin.ruleSerializer().types(ruleTypes);

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

    public PaperComponent(SokolPlugin platform, String id, Map<String, PaperSlot> slots, Map<String, PaperSystem> baseSystems, Collection<String> tags, StatLists stats) {
        super(id, slots, baseSystems, tags, stats);
        this.platform = platform;
    }

    public PaperComponent(SokolPlugin platform, Scoped<PaperComponent, PaperSlot, PaperSystem> o) {
        super(o);
        this.platform = platform;
    }

    public PaperComponent(PaperComponent o) {
        this(o.platform, o);
    }

    @Override public @NotNull SokolPlatform platform() { return platform; }
    @Override public @NotNull PaperComponent self() { return this; }

    public PaperTreeNode asTree() {
        return new PaperTreeNode(this).build();
    }

    @Override
    public boolean equals(Object o) {
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
