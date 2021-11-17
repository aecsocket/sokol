package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;

public abstract class SlotDisplayFeature<I extends SlotDisplayFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "slot_display";

    private final String padding;
    private final int paddingWidth;

    public SlotDisplayFeature(String padding, int paddingWidth) {
        this.padding = padding;
        this.paddingWidth = paddingWidth;
    }

    public String padding() { return padding; }
    public int paddingWidth() { return paddingWidth; }

    protected abstract int width(String text);

    public abstract class Instance extends AbstractInstance<N> {
        public Instance(N parent) {
            super(parent);
        }

        @Override public SlotDisplayFeature<I, N> type() { return SlotDisplayFeature.this; }

        protected abstract TypeToken<? extends CreateItemEvent<N>> eventCreateItem();

        @Override
        public void build(NodeEvent<N> event, StatIntermediate stats) {
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(eventCreateItem(), this::onCreateItem);
            });
        }

        private record KeyData(Component component, int width) {}

        private void lines(List<Component> lines, Locale locale, Map<String, KeyData> keyData, int longest, N node, Component indent, int depth) {
            var slots = node.value().slots().entrySet();

            for (var entry : slots) {
                String key = entry.getKey();
                Optional<N> optChild = node.node(key);
                platform().lc().lines(locale, lcKey("entry"),
                        "padding", padding.repeat((longest - keyData.get(key).width) / paddingWidth),
                        "indent", Components.repeat(indent, depth),
                        "key", platform().lc().safe(locale, Slot.renderKey(key)),
                        "value", optChild
                                .map(child -> platform().lc().safe(locale, lcKey("value.component"),
                                        "name", child.value().render(locale, platform().lc())))
                                .orElse(platform().lc().safe(locale, lcKey("value.empty"))))
                        .ifPresent(lines::addAll);
                optChild.ifPresent(child -> lines(lines, locale, keyData, longest, child, indent, depth + 1));
            }
        }

        private int makeKeyData(Map<String, KeyData> keyData, Locale locale, N node) {
            int longest = 0;
            for (var key : node.value().slots().keySet()) {
                if (keyData.containsKey(key))
                    continue;
                Component rendered = platform().lc().safe(locale, Slot.renderKey(key));
                int width = width(PlainTextComponentSerializer.plainText().serialize(rendered));
                keyData.put(key, new KeyData(rendered, width));
                if (width > longest)
                    longest = width;
            }

            for (var child : node.nodes().values()) {
                int childLongest = makeKeyData(keyData, locale, child);
                if (childLongest > longest)
                    longest = childLongest;
            }
            return longest;
        }

        private void onCreateItem(CreateItemEvent<N> event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            List<Component> lines = new ArrayList<>();
            Component indent = platform().lc().safe(locale, lcKey("indent"));

            Map<String, KeyData> keyData = new HashMap<>();
            int longest = makeKeyData(keyData, locale, event.node());
            lines(lines, locale, keyData, longest, event.node(), indent, 0);

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
