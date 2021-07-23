package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.ItemSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.PaperItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;

import static com.gitlab.aecsocket.sokol.paper.stat.ItemStat.*;

public final class PaperItemSystem extends ItemSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    private static final Stat<ItemDescriptor> stat = itemStat();

    public final class Instance extends ItemSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperItemSystem base() { return PaperItemSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override public PaperItemStack create(Locale locale) {
            return (PaperItemStack) super.create(locale);
        }
    }

    private final SokolPlugin platform;

    public PaperItemSystem(SokolPlugin platform) {
        super(stat);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static PaperSystem.Type type(SokolPlugin platform) {
        return cfg -> new PaperItemSystem(platform);
    }
}
