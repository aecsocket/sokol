package com.github.aecsocket.sokol.paper.feature;

import com.github.aecsocket.sokol.core.feature.StatDisplay;
import com.github.aecsocket.sokol.paper.*;
import io.leangen.geantyref.TypeToken;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;

public final class PaperStatDisplay extends StatDisplay<
    PaperStatDisplay, PaperStatDisplay.Profile, PaperStatDisplay.Profile.Data, PaperStatDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
> implements PaperFeature<PaperStatDisplay.Profile> {
    public static final String
        LISTENER_PRIORITY = "listener_priority",
        SECTIONS = "sections",
        PADDING = "padding";

    private final SokolPlugin platform;

    public PaperStatDisplay(SokolPlugin platform) {
        super(platform.i18n());
        this.platform = platform;
    }

    @Override protected PaperStatDisplay self() { return this; }
    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Profile setUp(ConfigurationNode node) throws SerializationException {
        String padding = node.node(PADDING).getString(" ");
        return new Profile(
            node.node(LISTENER_PRIORITY).getInt(),
            node.node(SECTIONS).get(new TypeToken<List<List<Format<?>>>>() {}, Collections.emptyList()),
            padding,
            platform.font().getWidth(padding) + 1
        );
    }

    public final class Profile extends StatDisplay<
        PaperStatDisplay, PaperStatDisplay.Profile, PaperStatDisplay.Profile.Data, PaperStatDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
    >.Profile implements PaperFeatureProfile<PaperStatDisplay, Profile.Data> {
        @Override protected Profile self() { return this; }

        public Profile(int listenerPriority, List<List<Format<?>>> sections, String padding, int paddingWidth) {
            super(listenerPriority, sections, padding, paddingWidth);
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

        public final class Data extends StatDisplay<
            PaperStatDisplay, PaperStatDisplay.Profile, PaperStatDisplay.Profile.Data, PaperStatDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
        >.Profile.Data implements PaperFeatureData<Profile, Data.Instance> {
            @Override protected Data self() { return this; }

            @Override
            public Instance asInstance(PaperTreeNode node) {
                return new Instance();
            }

            @Override
            public void save(PersistentDataContainer pdc, PersistentDataAdapterContext ctx) {}

            @Override
            protected int width(String text) {
                return platform.font().getWidth(text);
            }

            public final class Instance extends StatDisplay<
                PaperStatDisplay, PaperStatDisplay.Profile, PaperStatDisplay.Profile.Data, PaperStatDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
            >.Profile.Data.Instance implements PaperFeatureInstance<Data> {
                @Override
                public Instance copy() {
                    return new Instance();
                }
            }
        }
    }
}
