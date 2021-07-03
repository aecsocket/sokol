package com.gitlab.aecsocket.sokol.paper.system.impl;

import com.gitlab.aecsocket.sokol.core.system.SlotInfoSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class PaperSlotInfoSystem extends SlotInfoSystem implements PaperSystem {
    public final class Instance extends SlotInfoSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public @NotNull PaperSlotInfoSystem base() { return PaperSlotInfoSystem.this; }
        @Override public @NotNull SokolPlugin platform() { return platform; }
    }

    private final SokolPlugin platform;

    public PaperSlotInfoSystem(SokolPlugin platform) {
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public @NotNull Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node);
    }
}
