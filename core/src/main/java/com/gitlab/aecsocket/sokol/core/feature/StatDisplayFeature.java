package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.minecommons.core.Range;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.stat.Primitives;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;

public abstract class StatDisplayFeature<I extends StatDisplayFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "stat_display";

    public interface Format<T> {
        String type();
        String key();
        boolean accepts(Stat<?> stat);
        Component format(Locale locale, Stat.Node<T> node);
        default String lcKey() { return "stat_format." + type(); }
        default String lcKey(String key) { return lcKey() + "." + key; }
    }

    private final List<List<Format<?>>> sections;
    private final String padding;
    private final int paddingWidth;

    public StatDisplayFeature(List<List<Format<?>>> sections, String padding, int paddingWidth) {
        this.sections = sections;
        this.padding = padding;
        this.paddingWidth = paddingWidth;
    }

    public List<List<Format<?>>> sections() { return sections; }
    public String padding() { return padding; }
    public int paddingWidth() { return paddingWidth; }

    protected abstract int width(String text);

    public abstract class Instance extends AbstractInstance<N> {
        public Instance(N parent) {
            super(parent);
        }

        public Instance(Instance o) {
            super(o);
        }

        @Override public StatDisplayFeature<I, N> type() { return StatDisplayFeature.this; }

        protected abstract TypeToken<? extends CreateItemEvent<N>> eventCreateItem();

        @Override
        public void build(NodeEvent<N> event, StatIntermediate stats) {
            parent.treeData().ifPresent(treeData -> {
                var events = treeData.events();
                events.register(eventCreateItem(), this::onCreateItem);
            });
        }

        private <T> Optional<Component> lines(Locale locale, Format<T> format, StatMap stats, KeyData keyData, int longest) {
            String key = format.key();
            Stat.Node<?> rawNode = stats.get(key);
            if (!format.accepts(rawNode.stat()))
                return Optional.empty();

            @SuppressWarnings("unchecked")
            Stat.Node<T> node = (Stat.Node<T>) stats.get(key);
            if (node == null)
                return Optional.empty();

            Component renderedKey = keyData.component;
            Component renderedNode = format.format(locale, node);
            return platform().lc().get(locale, lcKey("entry"),
                    "indent", padding.repeat((longest - keyData.width) / paddingWidth),
                    "key", keyData.component,
                    "value", platform().lc().get(locale, Stat.renderFormatKey(key),
                            "value", renderedNode)
                            .orElse(renderedNode));
        }

        private record KeyData(Component component, int width) {}

        private void onCreateItem(CreateItemEvent<N> event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            List<Component> lines = new ArrayList<>();
            Optional<List<Component>> separator = platform().lc().lines(locale, lcKey("separator"));
            // TODO: if it is a complete item, show the final stat map
            // else show individual component stat maps
            StatMap stats = treeData(event.node()).stats();
            for (int i = 0; i < sections.size(); i++) {
                var section = sections.get(i);
                if (i > 0)
                    separator.ifPresent(lines::addAll);

                Map<String, KeyData> keyData = new HashMap<>();
                int longest = 0;
                for (var format : section) {
                    String key = format.key();
                    if (!stats.containsKey(key))
                        continue;
                    Component rendered = platform().lc().safe(locale, Stat.renderKey(key));
                    int width = width(PlainTextComponentSerializer.plainText().serialize(rendered));
                    keyData.put(key, new KeyData(rendered, width));
                    if (width > longest)
                        longest = width;
                }

                for (var format : section) {
                    lines(locale, format, stats, keyData.get(format.key()), longest)
                            .ifPresent(lines::add);
                }
            }
            event.item().addDescription(lines);
        }
    }

    @Override public String id() { return ID; }

    @Override public void configure(ConfigurationNode config) {}

    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return Optional.empty();
    }

    public interface FormatFactory {
        Format<?> create(SokolPlatform platform, ConfigurationNode config) throws SerializationException;
    }

    public static final class FormatRegistry {
        private final SokolPlatform platform;
        private final Map<String, FormatFactory> factories = new HashMap<>();

        public FormatRegistry(SokolPlatform platform) {
            this.platform = platform;
        }

        public Optional<Format<?>> get(String type, ConfigurationNode config) throws SerializationException {
            FormatFactory factory = factories.get(type);
            if (factory == null)
                return Optional.empty();
            return Optional.of(factory.create(platform, config));
        }

        public void register(String type, FormatFactory factory) {
            factories.put(type, factory);
        }
    }

    public static final class FormatSerializer implements TypeSerializer<Format<?>> {
        private final FormatRegistry registry;

        public FormatSerializer(FormatRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void serialize(Type type, @Nullable Format<?> obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else throw new UnsupportedOperationException();
        }

        @Override
        public Format<?> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String formatType = Serializers.require(node.node("type"), String.class);
            return registry.get(formatType, node)
                    .orElseThrow(() -> new SerializationException(node, type, "Invalid stat format type '" + formatType + "'"));
        }
    }

    public static final class Formats {
        private Formats() {}

        public static void registerAll(FormatRegistry registry) {
            registry.register(Number.TYPE, Number.FACTORY);
            registry.register(NumberBar.TYPE, NumberBar.FACTORY);
        }

        public record Number(
                SokolPlatform platform,
                String key,
                String format
        ) implements Format<java.lang.Number> {
            public static final String TYPE = "number";
            public static final FormatFactory FACTORY = (platform, config) -> new Number(platform,
                    require(config.node("key"), String.class),
                    require(config.node("format"), String.class)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Primitives.OfNumber; }

            @Override
            public Component format(Locale locale, Stat.Node<java.lang.Number> node) {
                List<Component> renderedNodes = new ArrayList<>();
                for (var cur = node; cur != null; cur = cur.next()) {
                    Stat.Value<?> raw = cur.value();
                    if (raw instanceof Primitives.OfNumber.BaseValue<?> val) {
                        renderedNodes.add(platform.lc().safe(locale, lcKey(raw instanceof Primitives.OfNumber.SetValue ? "set" : "op"),
                                "op", val.operator(),
                                "value", String.format(locale, format, val.wrappedValue().doubleValue())));
                    }
                }
                return Component.join(JoinConfiguration.separator(platform.lc().safe(locale, lcKey("separator"))), renderedNodes);
            }
        }

        public record NumberBar(
                SokolPlatform platform,
                String key,
                String format,
                int barLength,
                Range.Double barRange,
                String barPlaceholder,
                TextColor barFull,
                TextColor barEmpty
        ) implements Format<java.lang.Number> {
            public static final String TYPE = "number_bar";
            public static final FormatFactory FACTORY = (platform, config) -> new NumberBar(platform,
                    require(config.node("key"), String.class),
                    require(config.node("format"), String.class),
                    require(config.node("bar_length"), int.class),
                    require(config.node("bar_range"), Range.Double.class),
                    config.node("bar_placeholder").getString(" "),
                    config.node("bar_full").get(TextColor.class, NamedTextColor.WHITE),
                    config.node("bar_empty").get(TextColor.class, NamedTextColor.DARK_GRAY)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Primitives.OfNumber; }

            @Override
            public Component format(Locale locale, Stat.Node<java.lang.Number> node) {
                double num = node.compute().doubleValue();
                return platform.lc().safe(locale, lcKey(),
                        "bar", Components.bar(barLength, Numbers.clamp01((num - barRange.min()) / (barRange.max() - barRange.min())),
                                barPlaceholder, barFull, barEmpty),
                        "value", String.format(locale, format, num));
            }
        }
    }
}
