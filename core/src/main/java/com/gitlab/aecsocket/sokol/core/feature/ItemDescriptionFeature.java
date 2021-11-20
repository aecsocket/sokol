package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.stat.Primitives;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;

public abstract class ItemDescriptionFeature<I extends ItemDescriptionFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "item_description";
    public static final Primitives.OfString STAT_ITEM_NAME_KEY = Primitives.stringStat("item_name_key");

    private final int listenerPriority;

    public ItemDescriptionFeature(int listenerPriority) {
        this.listenerPriority = listenerPriority;
    }

    public int listenerPriority() { return listenerPriority; }

    public abstract class Instance extends AbstractInstance<N> {
        public Instance(N parent) {
            super(parent);
        }

        @Override public ItemDescriptionFeature<I, N> type() { return ItemDescriptionFeature.this; }

        protected abstract TypeToken<? extends CreateItemEvent<N>> eventCreateItem();

        @Override
        public void build(NodeEvent<N> event, StatIntermediate stats) {
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(eventCreateItem(), this::onCreateItem, listenerPriority);
            });
        }

        private void onCreateItem(CreateItemEvent<N> event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            N node = event.node();
            StatMap stats = treeData(node).stats();
            List<Component> lines = new ArrayList<>();

            stats.value(STAT_ITEM_NAME_KEY).flatMap(itemNameKey -> platform().lc().get(locale, itemNameKey))
                    .ifPresent(event.item()::name);

            node.value().renderDescription(locale, platform().lc()).ifPresent(desc -> {
                for (var line : desc) {
                    platform().lc().lines(locale, lcKey("line"),
                            "line", line)
                            .ifPresent(lines::addAll);
                }
            });

            event.item().addDescription(lines);
        }
    }

    @Override public String id() { return ID; }

    @Override public void configure(ConfigurationNode config) {}

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return Optional.empty();
    }
}
