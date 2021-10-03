package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.feature.LoadProvider;
import com.gitlab.aecsocket.sokol.core.feature.inbuilt.ItemFeature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperFeature;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.ItemDescriptor;
import com.gitlab.aecsocket.sokol.paper.wrapper.item.PaperItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;

import static com.gitlab.aecsocket.sokol.paper.stat.ItemStat.*;

public final class PaperItemFeature extends ItemFeature implements PaperFeature {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(ID, STATS);

    private static final Stat<ItemDescriptor> stat = itemStat("item");

    public final class Instance extends ItemFeature.Instance implements PaperFeature.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperItemFeature base() { return PaperItemFeature.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override public PaperItemStack create(Locale locale) {
            return (PaperItemStack) super.create(locale);
        }
    }

    private final SokolPlugin platform;

    public PaperItemFeature(SokolPlugin platform) {
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

    public static ConfigType type(SokolPlugin platform) {
        return cfg -> new PaperItemFeature(platform);
    }
}
