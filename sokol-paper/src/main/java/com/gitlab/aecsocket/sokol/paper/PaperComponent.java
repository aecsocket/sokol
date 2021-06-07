package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.component.BasicComponent;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public final class PaperComponent extends BasicComponent<PaperComponent, PaperSlot, PaperSystem> {
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
            Map<String, PaperSystem> systems = new HashMap<>();
            for (var entry : node.node("systems").childrenMap().entrySet()) {
                String systemId = (String) entry.getKey();
                PaperSystem.KeyedType systemType = plugin.systemTypes().get(systemId);
                if (systemType == null)
                    throw new SerializationException(node, type, "Could not find system [" + systemId + "]");
                try {
                    systems.put(systemId, systemType.create(entry.getValue()));
                } catch (SerializationException e) {
                    throw new SerializationException(node, type, "Could not create system [" + systemId + "]", e);
                }
            }
            return new PaperComponent(plugin,
                    ""+node.key(),
                    node.node("slots").get(new TypeToken<Map<String, PaperSlot>>() {}, Collections.emptyMap()),
                    systems,
                    node.node("tags").get(new TypeToken<Set<String>>() {}, Collections.emptySet())
            );
        }
    }

    private final SokolPlugin plugin;

    public PaperComponent(SokolPlugin plugin, String id, Map<String, PaperSlot> slots, Map<String, PaperSystem> baseSystems, Collection<String> tags) {
        super(id, slots, baseSystems, tags);
        this.plugin = plugin;
    }

    public PaperComponent(SokolPlugin plugin, Scoped<PaperComponent, PaperSlot, PaperSystem> o) {
        super(o);
        this.plugin = plugin;
    }

    public PaperComponent(PaperComponent o) {
        this(o.plugin, o);
    }

    @Override public @NotNull PaperComponent self() { return this; }

    public PaperTreeNode asTree() {
        return new PaperTreeNode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaperComponent that = (PaperComponent) o;
        return plugin.equals(that.plugin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), plugin);
    }

    @Override
    public String toString() {
        return "PaperComponent{" +
                "id='" + id + '\'' +
                ", slots=" + slots +
                ", baseSystems=" + baseSystems.keySet() +
                ", tags=" + tags +
                '}';
    }
}
