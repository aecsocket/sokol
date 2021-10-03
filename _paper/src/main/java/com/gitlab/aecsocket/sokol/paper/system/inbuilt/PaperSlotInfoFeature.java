package com.gitlab.aecsocket.sokol.paper.system.inbuilt;

import com.gitlab.aecsocket.sokol.core.feature.LoadProvider;
import com.gitlab.aecsocket.sokol.core.feature.inbuilt.SlotInfoFeature;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperFeature;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class PaperSlotInfoFeature extends SlotInfoFeature implements PaperFeature {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.empty(ID);

    public final class Instance extends SlotInfoFeature.Instance implements PaperFeature.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperSlotInfoFeature base() { return PaperSlotInfoFeature.this; }
        @Override public SokolPlugin platform() { return platform; }
    }

    private final SokolPlugin platform;

    public PaperSlotInfoFeature(SokolPlugin platform, int listenerPriority) {
        super(listenerPriority);
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
        return cfg -> new PaperSlotInfoFeature(platform,
                cfg.node(keyListenerPriority).getInt());
    }
}
