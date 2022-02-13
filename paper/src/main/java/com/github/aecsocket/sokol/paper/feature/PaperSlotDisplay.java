package com.github.aecsocket.sokol.paper.feature;

import com.github.aecsocket.sokol.core.feature.SlotDisplay;
import com.github.aecsocket.sokol.paper.*;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.List;

public final class PaperSlotDisplay extends SlotDisplay<
        PaperSlotDisplay, PaperSlotDisplay.Profile, PaperSlotDisplay.Profile.Data, PaperSlotDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
> implements PaperFeature<PaperSlotDisplay.Profile> {
    public static final String
        LISTENER_PRIORITY = "listener_priority",
        DISPLAY_ORDER = "display_order",
        FORCE_START = "force_start",
        FORCE_END = "force_end",
        PADDING = "padding";

    private final SokolPlugin platform;

    public PaperSlotDisplay(SokolPlugin platform) {
        super(platform.i18n());
        this.platform = platform;
    }

    @Override protected PaperSlotDisplay self() { return this; }
    @Override public SokolPlugin platform() { return platform; }

    @Override
    public Profile setUp(ConfigurationNode node) throws SerializationException {
        String padding = node.node(PADDING).getString(" ");
        return new Profile(
            node.node(LISTENER_PRIORITY).getInt(),
            node.node(DISPLAY_ORDER).get(DisplayOrder.class, DisplayOrder.BROADEST_FIRST),
            node.node(FORCE_START).getList(String.class, Collections.emptyList()),
            node.node(FORCE_END).getList(String.class, Collections.emptyList()),
            padding,
            platform.font().getWidth(padding) + 1
        );
    }

    public final class Profile extends SlotDisplay<
            PaperSlotDisplay, PaperSlotDisplay.Profile, PaperSlotDisplay.Profile.Data, PaperSlotDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
    >.Profile implements PaperFeatureProfile<PaperSlotDisplay, Profile.Data> {
        @Override protected Profile self() { return this; }

        public Profile(int listenerPriority, DisplayOrder displayOrder, List<String> forceStart, List<String> forceEnd, String padding, int paddingWidth) {
            super(listenerPriority, displayOrder, forceStart, forceEnd, padding, paddingWidth);
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

        public final class Data extends SlotDisplay<
                PaperSlotDisplay, PaperSlotDisplay.Profile, PaperSlotDisplay.Profile.Data, PaperSlotDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
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

            public final class Instance extends SlotDisplay<
                    PaperSlotDisplay, PaperSlotDisplay.Profile, PaperSlotDisplay.Profile.Data, PaperSlotDisplay.Profile.Data.Instance, PaperTreeNode, PaperItemStack
            >.Profile.Data.Instance implements PaperFeatureInstance<Data> {
                @Override
                public Instance copy() {
                    return new Instance();
                }
            }
        }
    }
}
