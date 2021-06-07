package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.component.System;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public abstract class PaperSystem implements System.Base<PaperSystem.Instance> {
    public interface Type {
        PaperSystem create(ConfigurationNode node) throws SerializationException;
    }

    public interface KeyedType extends Type, Keyed {}

    public static abstract class Instance implements System<PaperSystem> {
        private final PaperSystem base;

        public Instance(PaperSystem base) {
            this.base = base;
        }

        @Override public PaperSystem base() { return base; }

        public abstract PersistentDataContainer save(PersistentDataAdapterContext ctx);
    }

    private final SokolPlugin plugin;
    private final String id;

    public PaperSystem(SokolPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public SokolPlugin plugin() { return plugin; }
    @Override public @NotNull String id() { return id; }

    public abstract Instance load(PersistentDataContainer data);
}
