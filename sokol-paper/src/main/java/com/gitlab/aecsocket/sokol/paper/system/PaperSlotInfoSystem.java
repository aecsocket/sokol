package com.gitlab.aecsocket.sokol.paper.system;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.system.SlotInfoSystem;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class PaperSlotInfoSystem extends SlotInfoSystem<PaperTreeNode> implements PaperSystem {
    public static final Type TYPE = (plugin, node) -> new PaperSlotInfoSystem(plugin);

    private final SokolPlugin platform;

    public PaperSlotInfoSystem(SokolPlugin platform) {
        this.platform = platform;
    }

    public final class Instance extends SlotInfoSystem.Instance<PaperTreeNode> implements PaperSystem.Instance {
        public Instance(PaperTreeNode parent) {
            super(parent);
        }

        @Override public @NotNull PaperSlotInfoSystem base() { return PaperSlotInfoSystem.this; }
        @Override public @NotNull SokolPlugin platform() { return platform; }
        @Override public PersistentDataContainer save(PersistentDataAdapterContext ctx) { return null; }
        @Override public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {}
    }

    @Override
    public @NotNull Instance create(PaperTreeNode node, Component component) {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public PaperSystem.@NotNull Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node);
    }
}
