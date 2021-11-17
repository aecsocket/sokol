package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.minecommons.core.Range;
import com.gitlab.aecsocket.minecommons.core.expressions.math.MathNode;
import com.gitlab.aecsocket.minecommons.core.expressions.node.EvaluationException;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.stat.*;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;
import static com.gitlab.aecsocket.minecommons.core.Components.barSection;

public abstract class StatDisplayFeature<I extends StatDisplayFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "stat_display";

    public interface Format<T> {
        String type();
        String key();
        @Nullable String customLcKey();
        boolean accepts(Stat<?> stat);
        Component format(Locale locale, Stat.Node<? extends T> node);
        default String lcKey() { return "stat_format." + (customLcKey() == null ? type() : customLcKey()); }
        default String lcKey(String key) { return lcKey() + "." + key; }
    }

    public static class FormatRenderException extends RuntimeException {
        public FormatRenderException() {}
        public FormatRenderException(String message) { super(message); }
        public FormatRenderException(String message, Throwable cause) { super(message, cause); }
        public FormatRenderException(Throwable cause) { super(cause); }
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
            Component renderedNode;
            try {
                renderedNode = format.format(locale, node);
            } catch (FormatRenderException e) {
                throw new ItemCreationException("Could not render value for stat '" + key + "'", e);
            }
            return platform().lc().get(locale, lcKey("entry"),
                    "padding", padding.repeat((longest - keyData.width) / paddingWidth),
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
                    if (!stats.containsKey(key) || keyData.containsKey(key))
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

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return Optional.empty();
    }

    public interface FormatFactory {
        Format<?> create(SokolPlatform platform, ConfigurationNode config) throws SerializationException, FormatCreationException;
    }

    public static class FormatCreationException extends Exception {
        public FormatCreationException() {}
        public FormatCreationException(String message) { super(message); }
        public FormatCreationException(String message, Throwable cause) { super(message, cause); }
        public FormatCreationException(Throwable cause) { super(cause); }
    }

    public static final class FormatRegistry {
        private final SokolPlatform platform;
        private final Map<String, FormatFactory> factories = new HashMap<>();

        public FormatRegistry(SokolPlatform platform) {
            this.platform = platform;
        }

        public Optional<Format<?>> get(String type, ConfigurationNode config) throws SerializationException, FormatCreationException {
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
            try {
                return registry.get(formatType, node)
                        .orElseThrow(() -> new SerializationException(node, type, "Invalid stat format type '" + formatType + "'"));
            } catch (FormatCreationException e) {
                throw new SerializationException(node, type, e);
            }
        }
    }

    public static final class Formats {
        private Formats() {}

        public static void registerAll(FormatRegistry registry) {
            registry.register(Raw.TYPE, Raw.FACTORY);
            registry.register(Number.TYPE, Number.FACTORY);
            registry.register(NumberBar.TYPE, NumberBar.FACTORY);
            registry.register(OfVector2.TYPE, OfVector2.FACTORY);
            registry.register(Vector2SeparateBars.TYPE, Vector2SeparateBars.FACTORY);
            registry.register(Vector2ContinuousBar.TYPE, Vector2ContinuousBar.FACTORY);
        }

        private static double tryExpress(double num, @Nullable MathNode math) {
            if (math != null) {
                try {
                    return math.set("v", num).eval();
                } catch (EvaluationException e) {
                    throw new IllegalArgumentException("Could not evaluate expression", e);
                }
            }
            return num;
        }

        private static double barValue(double v, Range.Double range) {
            return Numbers.clamp01((v - range.min()) / (range.max() - range.min()));
        }

        public record Raw(
                SokolPlatform platform,
                String key,
                @Nullable String customLcKey,
                boolean rendered
        ) implements Format<Object> {
            public static final String TYPE = "raw";
            public static final FormatFactory FACTORY = (platform, config) -> new Raw(platform,
                    require(config.node("key"), String.class),
                    config.node("custom_lc_key").getString(),
                    config.node("rendered").getBoolean(true)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return true; }

            @Override
            public Component format(Locale locale, Stat.Node<?> node) {
                return join(separator(platform.lc().safe(locale, lcKey("separator"))),
                        node.stream()
                                .map(cur -> rendered ? cur.value().render(locale, platform.lc()) : text(cur.value().toString()))
                                .collect(Collectors.toList()));
            }
        }

        @ConfigSerializable
        public static final class NumberOptions {
            private final @Required String format;
            private final @Nullable MathNode expression;

            public NumberOptions(String format, @Nullable MathNode expression) {
                this.format = format;
                this.expression = expression;
            }

            private NumberOptions() {
                format = "";
                expression = null;
            }

            public String format() { return format; }
            public MathNode expression() { return expression; }

            public double express(double value) {
                return tryExpress(value, expression);
            }

            public String format(Locale locale, double value) {
                try {
                    return String.format(locale, format, value);
                } catch (IllegalFormatException e) {
                    throw new FormatRenderException("Invalid format '" + format + "'", e);
                }
            }

            public String render(Locale locale, double value) {
                return format(locale, express(value));
            }
        }

        public record Number(
                SokolPlatform platform,
                String key,
                @Nullable String customLcKey,
                NumberOptions options
        ) implements Format<java.lang.Number> {
            public static final String TYPE = "number";
            public static final FormatFactory FACTORY = (platform, config) -> new Number(platform,
                    require(config.node("key"), String.class),
                    config.node("custom_lc_key").getString(),
                    require(config.node("options"), NumberOptions.class)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Primitives.OfNumber; }

            @Override
            public Component format(Locale locale, Stat.Node<? extends java.lang.Number> node) {
                List<Component> renderedNodes = new ArrayList<>();
                for (var cur = node; cur != null; cur = cur.next()) {
                    Stat.Value<?> raw = cur.value();
                    if (raw instanceof Primitives.OfNumber.BaseValue<?> val) {
                        renderedNodes.add(platform.lc().safe(locale, lcKey(raw instanceof Primitives.OfNumber.SetValue ? "set" : "op"),
                                "op", val.operator(),
                                "value", options.render(locale, val.wrappedValue().doubleValue())));
                    }
                }
                return join(separator(platform.lc().safe(locale, lcKey("separator"))), renderedNodes);
            }
        }

        @ConfigSerializable
        public static final class BarOptions {
            private final @Required int length;
            private final @Required Range.Double range;
            private final String placeholder;
            private final TextColor fill;

            public BarOptions(int length, Range.Double range, String placeholder, TextColor fill) {
                this.length = length;
                this.range = range;
                this.placeholder = placeholder;
                this.fill = fill;
            }

            private BarOptions() {
                length = 0;
                range = Range.ofDouble(0, 0);
                placeholder = " ";
                fill = NamedTextColor.DARK_GRAY;
            }

            public int length() { return length; }
            public Range.Double range() { return range; }
            public String placeholder() { return placeholder; }
            public TextColor empty() { return fill; }

            public Component render(Components.BarSection... barSections) {
                return Components.bar(length, placeholder, fill, barSections);
            }
        }

        public record NumberBar(
                SokolPlatform platform,
                String key,
                @Nullable String customLcKey,
                NumberOptions options,
                @Nullable MathNode expressionBar,
                BarOptions bar,
                TextColor barColor
        ) implements Format<java.lang.Number> {
            public static final String TYPE = "number_bar";
            public static final FormatFactory FACTORY = (platform, config) -> new NumberBar(platform,
                    require(config.node("key"), String.class),
                    config.node("custom_lc_key").getString(),
                    require(config.node("options"), NumberOptions.class),
                    config.node("expression_bar").get(MathNode.class),
                    require(config.node("bar"), BarOptions.class),
                    require(config.node("bar_color"), TextColor.class)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Primitives.OfNumber; }

            @Override
            public Component format(Locale locale, Stat.Node<? extends java.lang.Number> node) {
                Range.Double range = bar.range;
                double num = node.compute().doubleValue();
                double barNum = tryExpress(num, expressionBar);
                return platform.lc().safe(locale, lcKey(),
                        "bar", bar.render(barSection(barValue(barNum, bar.range), barColor)),
                        "value", options.render(locale, num));
            }
        }

        public record OfVector2(
                SokolPlatform platform,
                String key,
                @Nullable String customLcKey,
                NumberOptions optionsX,
                NumberOptions optionsY
        ) implements Format<Vector2> {
            public static final String TYPE = "vector2";
            public static final FormatFactory FACTORY = (platform, config) -> new OfVector2(platform,
                    require(config.node("key"), String.class),
                    config.node("custom_lc_key").getString(),
                    require(config.node("options_x"), NumberOptions.class),
                    require(config.node("options_y"), NumberOptions.class)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Vectors.OfVector; }

            @Override
            public Component format(Locale locale, Stat.Node<? extends Vector2> node) {
                List<Component> renderedNodes = new ArrayList<>();
                for (var cur = node; cur != null; cur = cur.next()) {
                    Stat.Value<?> raw = cur.value();
                    if (raw instanceof Vectors.OfVector2.BaseValue val) {
                        Vector2 vec = val.value();
                        renderedNodes.add(platform.lc().safe(locale, lcKey(raw instanceof Vectors.OfVector.SetValue ? "set" : "op"),
                                "op", val.operator(),
                                "x", optionsX.render(locale, vec.x()),
                                "y", optionsY.render(locale, vec.y())));
                    }
                }
                return join(separator(platform.lc().safe(locale, lcKey("separator"))), renderedNodes);
            }
        }

        public record Vector2SeparateBars(
                SokolPlatform platform,
                String key,
                @Nullable String customLcKey,
                NumberOptions optionsX,
                NumberOptions optionsY,
                @Nullable MathNode expressionBarX,
                @Nullable MathNode expressionBarY,
                BarOptions barX,
                BarOptions barY,
                TextColor barXColor,
                TextColor barYColor
        ) implements Format<Vector2> {
            public static final String TYPE = "vector2_separate_bars";
            public static final FormatFactory FACTORY = (platform, config) -> new Vector2SeparateBars(platform,
                    require(config.node("key"), String.class),
                    config.node("custom_lc_key").getString(),
                    require(config.node("options_x"), NumberOptions.class),
                    require(config.node("options_y"), NumberOptions.class),
                    config.node("expression_bar_x").get(MathNode.class),
                    config.node("expression_bar_y").get(MathNode.class),
                    require(config.node("bar_x"), BarOptions.class),
                    require(config.node("bar_y"), BarOptions.class),
                    require(config.node("bar_x_color"), TextColor.class),
                    require(config.node("bar_y_color"), TextColor.class)
            );

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Vectors.OfVector2; }

            @Override
            public Component format(Locale locale, Stat.Node<? extends Vector2> node) {
                Vector2 vec = node.compute();
                Vector2 barVec = new Vector2(
                        expressionBarX == null ? vec.x() : tryExpress(vec.x(), expressionBarX),
                        expressionBarY == null ? vec.y() : tryExpress(vec.y(), expressionBarY)
                );
                Range.Double rangeX = barX.range;
                Range.Double rangeY = barY.range;
                return platform.lc().safe(locale, lcKey(),
                        "bar_x", barX.render(barSection(barValue(barVec.x(), barX.range), barXColor)),
                        "bar_y", barY.render(barSection(barValue(barVec.y(), barY.range), barYColor)),
                        "x", optionsX.render(locale, vec.x()),
                        "y", optionsY.render(locale, vec.y()));
            }
        }

        public record Vector2ContinuousBar(
                SokolPlatform platform,
                String key,
                @Nullable String customLcKey,
                NumberOptions optionsX,
                NumberOptions optionsY,
                @Nullable MathNode expressionBarX,
                @Nullable MathNode expressionBarY,
                int[] barOrder,
                BarOptions bar,
                TextColor barXColor,
                TextColor barYColor
        ) implements Format<Vector2> {
            public static final String TYPE = "vector2_continuous_bar";
            public static final FormatFactory FACTORY = (platform, config) -> {
                int[] barOrder = require(config.node("bar_order"), int[].class);
                if (barOrder.length != 3)
                    throw new FormatCreationException("Bar order must be array of 3 elements");
                return new Vector2ContinuousBar(platform,
                        require(config.node("key"), String.class),
                        config.node("custom_lc_key").getString(),
                        require(config.node("options_x"), NumberOptions.class),
                        require(config.node("options_y"), NumberOptions.class),
                        config.node("expression_bar_x").get(MathNode.class),
                        config.node("expression_bar_y").get(MathNode.class),
                        barOrder,
                        require(config.node("bar"), BarOptions.class),
                        require(config.node("bar_x_color"), TextColor.class),
                        require(config.node("bar_y_color"), TextColor.class)
                );
            };

            @Override public String type() { return TYPE; }
            @Override public boolean accepts(Stat<?> stat) { return stat instanceof Vectors.OfVector2; }

            @Override
            public Component format(Locale locale, Stat.Node<? extends Vector2> node) {
                Vector2 vec = node.compute();
                Vector2 barVec = new Vector2(
                        expressionBarX == null ? vec.x() : tryExpress(vec.x(), expressionBarX),
                        expressionBarY == null ? vec.y() : tryExpress(vec.y(), expressionBarY)
                );
                vec = new Vector2(
                        optionsX.express(vec.x()),
                        optionsY.express(vec.y())
                );
                Range.Double range = bar.range;
                double bar1 = barValue(barVec.x(), bar.range);
                double bar2 = barValue(barVec.y(), bar.range);

                Components.BarSection[] sections = new Components.BarSection[] {
                        barSection(1 - bar1 - bar2, bar.fill),
                        barSection(bar1, barXColor),
                        barSection(bar2, barYColor)
                };
                return platform.lc().safe(locale, lcKey(),
                        "bar", Components.bar(bar.length, bar.placeholder, sections[barOrder[2]].color(),
                                sections[barOrder[0]],
                                sections[barOrder[1]]),
                        "x", optionsX.format(locale, vec.x()),
                        "y", optionsY.format(locale, vec.y()));
            }
        }
    }
}
