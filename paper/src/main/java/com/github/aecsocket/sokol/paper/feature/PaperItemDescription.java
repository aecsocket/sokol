package com.github.aecsocket.sokol.paper.feature;

import com.github.aecsocket.sokol.core.feature.ItemDescription;
import com.github.aecsocket.sokol.paper.*;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class PaperItemDescription extends ItemDescription<
    PaperItemDescription, PaperItemDescription.Profile, PaperItemDescription.Profile.Data, PaperItemDescription.Profile.Instance, PaperTreeNode, PaperItemStack
> implements PaperFeature<PaperItemDescription.Profile> {
    public static final String
        LISTENER_PRIORITY = "listener_priority";

    private final SokolPlugin platform;

    public PaperItemDescription(SokolPlugin platform) {
        super(platform.i18n());
        this.platform = platform;
    }

    @Override protected PaperItemDescription self() { return this; }
    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Profile setUp(ConfigurationNode node) throws SerializationException {
        return new Profile(
            node.node(LISTENER_PRIORITY).getInt()
        );
    }

    public final class Profile extends ItemDescription<
        PaperItemDescription, PaperItemDescription.Profile, PaperItemDescription.Profile.Data, PaperItemDescription.Profile.Instance, PaperTreeNode, PaperItemStack
    >.Profile implements PaperFeatureProfile<PaperItemDescription, Profile.Data> {
        @Override protected Profile self() { return this; }

        public Profile(int listenerPriority) {
            super(listenerPriority);
        }

        @Override
        public Data setUp() {
            return new Data();
        }

        @Override
        public Data load(ConfigurationNode node) throws SerializationException {
            return new Data();
        }

        @Override
        public Data load(PersistentDataContainer pdc) {
            return new Data();
        }

        public final class Data extends ItemDescription<
            PaperItemDescription, PaperItemDescription.Profile, PaperItemDescription.Profile.Data, PaperItemDescription.Profile.Instance, PaperTreeNode, PaperItemStack
        >.Profile.Data implements PaperFeatureData<Profile, Instance> {
            @Override
            public Instance asInstance(PaperTreeNode node) {
                return new Instance();
            }

            @Override
            public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) {}
        }

        public final class Instance extends ItemDescription<
            PaperItemDescription, PaperItemDescription.Profile, PaperItemDescription.Profile.Data, PaperItemDescription.Profile.Instance, PaperTreeNode, PaperItemStack
        >.Profile.Instance implements PaperFeatureInstance<Profile, Data> {
            @Override
            public Data asData() {
                return new Data();
            }

            @Override
            public Instance copy() {
                return new Instance();
            }
        }
    }
}
