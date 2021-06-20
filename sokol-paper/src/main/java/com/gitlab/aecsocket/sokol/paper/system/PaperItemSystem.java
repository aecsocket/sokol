package com.gitlab.aecsocket.sokol.paper.system;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;

public final class PaperItemSystem extends ItemSystem<PaperTreeNode> implements PaperSystem {
    public static final PaperSystem.Type TYPE = (plugin, node) -> new PaperItemSystem(plugin);
    private static final Stat<ItemDescriptor> stat = new ItemDescriptor.Stat(null);

    private final SokolPlugin platform;

    public PaperItemSystem(SokolPlugin platform) {
        super(stat);
        this.platform = platform;
    }

    public final class Instance extends ItemSystem.Instance<PaperTreeNode> implements PaperSystem.Instance {
        public Instance(PaperTreeNode parent) {
            super(parent);
        }

        @Override public @NotNull PaperItemSystem base() { return PaperItemSystem.this; }
        @Override public @NotNull SokolPlugin platform() { return platform; }
        @Override public PersistentDataContainer save(PersistentDataAdapterContext ctx) { return null; }
        @Override public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {}

        @Override public PaperItemStack create(Locale locale) { return (PaperItemStack) super.create(locale); }
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
    public @NotNull Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node);
    }
}
