package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.impl.AbstractNode;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class SlotDisplayFeature<I extends SlotDisplayFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "slot_display";

    public enum Order {
        DEEPEST_FIRST,
        BROADEST_FIRST,
        NATURAL
    }

    private final int listenerPriority;
    private final Order order;
    private final List<String> orderOverride;
    private final String padding;
    private final int paddingWidth;

    public SlotDisplayFeature(int listenerPriority, Order order, List<String> orderOverride, String padding, int paddingWidth) {
        this.listenerPriority = listenerPriority;
        this.order = order;
        this.orderOverride = orderOverride;
        this.padding = padding;
        this.paddingWidth = paddingWidth;
    }

    public int listenerPriority() { return listenerPriority; }
    public Order order() { return order; }
    public List<String> orderOverride() { return orderOverride; }
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
                events.register(eventCreateItem(), this::onCreateItem, listenerPriority);
            });
        }

        private record KeyData(Component component, int width) {}

        private int linesSetup(Map<String, KeyData> keyData, Map<NodePath, Integer> slotDepths, Locale locale, N node, AtomicInteger longest) {
            var slots = node.value().slots();
            int size = 0;
            for (var entry : slots.entrySet()) {
                String key = entry.getKey();
                size += 1 + node.node(key)
                        .map(c -> linesSetup(keyData, slotDepths, locale, c, longest))
                        .orElse(0);
                if (keyData.containsKey(key))
                    continue;
                Component rendered = platform().lc().safe(locale, Slot.renderKey(key));
                int width = width(PlainTextComponentSerializer.plainText().serialize(rendered));
                keyData.put(key, new KeyData(rendered, width));
                if (width > longest.get())
                    longest.set(width);
            }
            slotDepths.put(node.path(), size);
            return size;
        }

        private void lines(List<Component> lines, Locale locale, Map<String, KeyData> keyData, Map<NodePath, Integer> slotDepths, int longest, N node, Component indent, int depth) {
            var slots = node.value().slots();
            List<String> slotOrder = new ArrayList<>(orderOverride);

            var addStream = slots.keySet().stream()
                    .filter(key -> !slotOrder.contains(key));
            if (order != Order.NATURAL)
                addStream = addStream.sorted(Comparator.comparingInt(key -> slotDepths.getOrDefault(node.path().append(key), 0) *
                        (order == Order.DEEPEST_FIRST ? -1 : 1)));
            addStream.forEach(slotOrder::add);

            for (var key : slotOrder) {
                Slot slot = slots.get(key);
                if (slot == null)
                    continue;
                Optional<N> optChild = node.node(key);
                platform().lc().lines(locale, lcKey("entry"),
                        "padding", padding.repeat((longest - keyData.get(key).width) / paddingWidth),
                        "indent", Components.repeat(indent, depth),
                        "key", platform().lc().safe(locale, Slot.renderKey(key)),
                        "value", optChild
                                .map(child -> platform().lc().safe(locale, lcKey("value.component"),
                                        "name", child.value().render(locale, platform().lc())))
                                .orElse(AbstractNode.required(slot)
                                        ? platform().lc().safe(locale, lcKey("value.required"))
                                        : platform().lc().safe(locale, lcKey("value.empty"))))
                        .ifPresent(lines::addAll);
                optChild.ifPresent(child -> lines(lines, locale, keyData, slotDepths, longest, child, indent, depth + 1));
            }
        }

        private void onCreateItem(CreateItemEvent<N> event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            List<Component> lines = new ArrayList<>();
            Component indent = platform().lc().safe(locale, lcKey("indent"));

            Map<String, KeyData> keyData = new HashMap<>();
            Map<NodePath, Integer> slotDepths = new HashMap<>();
            AtomicInteger longest = new AtomicInteger();
            linesSetup(keyData, slotDepths, locale, event.node(), longest);
            lines(lines, locale, keyData, slotDepths, longest.get(), event.node(), indent, 0);

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
