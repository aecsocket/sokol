package com.gitlab.aecsocket.sokol.paper.system;

import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.util.Objects;

public interface PaperSystem extends System {
    final class Serializer implements TypeSerializer<PaperSystem.Instance> {
        private final SokolPlugin plugin;
        private PaperTreeNode base;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        public PaperTreeNode base() { return base; }
        public void base(PaperTreeNode component) { this.base = component; }

        @Override
        public void serialize(java.lang.reflect.Type type, PaperSystem.@Nullable Instance obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                obj.save(type, node);
            }
        }

        @Override
        public Instance deserialize(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            if (base == null)
                throw new SerializationException("No base component provided");

            String id = Objects.toString(node.key());
            PaperSystem system = base.value().baseSystem(id)
                    .orElseThrow(() -> new SerializationException("No system with ID [" + id + "] on component [" + base.value().id() + "]"));
            return system.load(base, type, node);
        }
    }

    interface Instance extends System.Instance {
        @Override PaperSystem base();

        default PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException { return null; }
        default void save(java.lang.reflect.Type type, ConfigurationNode node) {}
    }

    interface Type {
        PaperSystem create(SokolPlugin plugin, ConfigurationNode node) throws SerializationException;
    }

    interface KeyedType extends Type, Keyed {}

    Instance load(PaperTreeNode node, PersistentDataContainer data) throws IllegalArgumentException;
    Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException;
}
