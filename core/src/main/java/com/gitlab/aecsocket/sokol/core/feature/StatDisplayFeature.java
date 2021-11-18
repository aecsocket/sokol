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
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import com.gitlab.aecsocket.sokol.core.stat.*;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;
import static com.gitlab.aecsocket.minecommons.core.Components.barSection;

public abstract class StatDisplayFeature<I extends StatDisplayFeature<I, N>.Instance, N extends Node.Scoped<N, ?, ?>> extends AbstractFeature<I, N> {
    public static final String ID = "stat_display";

    public interface Format<T> {
        String id();
        String key();
        boolean accepts(Stat<?> stat);
        <V extends T> Component format(Locale locale, Localizer lc, Stat.Node<V> node);
        default void validate() throws FormatValidationException {}
    }

    public static abstract class AbstractFormat<T> implements Format<T> {
        private final @Required String key;
        private final @Nullable String customLcKey;

        public AbstractFormat(String key, @Nullable String customLcKey) {
            this.key = key;
            this.customLcKey = customLcKey;
        }

        private AbstractFormat() {
            key = "";
            customLcKey = null;
        }

        @Override public String key() { return key; }
        public String customLcKey() { return customLcKey; }

        protected String lcKey() { return "stat_format." + (customLcKey == null ? "default." : customLcKey + ".") + id(); }
        protected String lcKey(String key) { return lcKey() + "." + key; }

        protected double tryExpress(double num, @Nullable MathNode math) {
            if (math != null) {
                try {
                    return math.set("v", num).eval();
                } catch (EvaluationException e) {
                    throw new FormatRenderException("Could not evaluate expression", e);
                }
            }
            return num;
        }

        protected double percent(double val, Range.Double range) {
            return Numbers.clamp01((val - range.min()) / (range.max() - range.min()));
        }

        public interface Renderer<V> {
            @Nullable Component render(Stat.Value<V> val);
        }

        protected <V> Component join(Locale locale, Localizer lc, Stat.Node<V> node, Renderer<V> renderer) {
            List<Component> rendered = new ArrayList<>();
            for (var cur : node) {
                Component component = renderer.render(cur.value());
                if (component != null)
                    rendered.add(component);
            }
            return Component.join(separator(lc.safe(locale, lcKey("separator"))), rendered);
        }

        protected <V> Component computeOrJoin(Locale locale, Localizer lc, boolean compute, V value, Stat.Node<V> node, Renderer<V> renderer, Function<V, Component> computeRenderer) {
            return compute
                    ? computeRenderer.apply(value)
                    : join(locale, lc, node, renderer);
        }

        protected <V> Component computeOrJoin(Locale locale, Localizer lc, boolean compute, V value, Stat.Node<V> node, Renderer<V> renderer) {
            return computeOrJoin(locale, lc, compute, value, node, renderer, v -> node.stat().renderValue(locale, lc, v));
        }

        protected <V> Component computeOrJoin(Locale locale, Localizer lc, boolean compute, Stat.Node<V> node, Renderer<V> renderer, Function<V, Component> computeRenderer) {
            return computeOrJoin(locale, lc, compute, node.compute(), node, renderer, computeRenderer);
        }

        protected <V> Component computeOrJoin(Locale locale, Localizer lc, boolean compute, Stat.Node<V> node, Renderer<V> renderer) {
            return computeOrJoin(locale, lc, compute, node.compute(), node, renderer, v -> node.stat().renderValue(locale, lc, v));
        }
    }

    public static class FormatRenderException extends RuntimeException {
        public FormatRenderException() {}
        public FormatRenderException(String message) { super(message); }
        public FormatRenderException(String message, Throwable cause) { super(message, cause); }
        public FormatRenderException(Throwable cause) { super(cause); }
    }

    public static class FormatValidationException extends Exception {
        public FormatValidationException() {}
        public FormatValidationException(String message) { super(message); }
        public FormatValidationException(String message, Throwable cause) { super(message, cause); }
        public FormatValidationException(Throwable cause) { super(cause); }
    }

    private final int listenerPriority;
    private final List<List<Format<?>>> sections;
    private final String padding;
    private final int paddingWidth;

    public StatDisplayFeature(int listenerPriority, List<List<Format<?>>> sections, String padding, int paddingWidth) {
        this.listenerPriority = listenerPriority;
        this.sections = sections;
        this.padding = padding;
        this.paddingWidth = paddingWidth;
    }

    public int listenerPriority() { return listenerPriority; }
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
                events.register(eventCreateItem(), this::onCreateItem, listenerPriority);
            });
        }

        private record KeyData(Component component, int width) {}

        private <T> Optional<Component> lines(Locale locale, Format<T> format, StatMap stats, KeyData keyData, int longest) {
            String key = format.key();
            Stat.Node<?> rawNode = stats.get(key);
            if (rawNode == null)
                return Optional.empty();
            if (!format.accepts(rawNode.stat()))
                throw new ItemCreationException("Cannot create format for stat '" + key +
                        "': format '" + format.id() + "' does not accept stats of type " + rawNode.stat().getClass().getName());
            @SuppressWarnings("unchecked")
            Stat.Node<T> node = (Stat.Node<T>) rawNode;

            Component renderedKey = keyData.component;
            Component renderedNode;
            try {
                renderedNode = format.format(locale, platform().lc(), node);
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

        private void render(Locale locale, List<Component> lines, @Nullable List<Component> separator, StatMap stats) {
            for (int i = 0; i < sections.size(); i++) {
                var section = sections.get(i);
                if (i > 0 && separator != null)
                    lines.addAll(separator);

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
        }

        private void onCreateItem(CreateItemEvent<N> event) {
            if (!parent.isRoot())
                return;
            Locale locale = event.locale();
            List<Component> lines = new ArrayList<>();
            List<Component> separator = platform().lc().lines(locale, lcKey("separator")).orElse(null);

            render(locale, lines, separator, treeData(event.node()).stats());

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

    public record FormatType(String id, Class<? extends Format<?>> formatType) implements Keyed {
        public static String renderKey(String id) {
            return "stat_format." + id + ".name";
        }

        @Override
        public Component render(Locale locale, Localizer lc) {
            return lc.safe(locale, renderKey(id));
        }
    }

    public static final class FormatSerializer implements TypeSerializer<Format<?>> {
        private final Registry<FormatType> registry;

        public FormatSerializer(Registry<FormatType> registry) {
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
            Format<?> format = node.get(registry.get(formatType)
                    .orElseThrow(() -> new SerializationException(node, type, "Invalid stat format type '" + formatType + "'"))
                    .formatType);
            if (format == null)
                throw new SerializationException(node, type, "null");
            try {
                format.validate();
            } catch (FormatValidationException e) {
                throw new SerializationException(node, type, e);
            }
            return format;
        }
    }

    public static final class Formats {
        private Formats() {}

        public static void registerAll(Registry<FormatType> registry) {
            registry.register(Raw.TYPE);
            registry.register(OfNumber.TYPE);
            registry.register(NumberBar.TYPE);
            registry.register(OfVector2.TYPE);
            registry.register(Vector2SeparateBars.TYPE);
            registry.register(Vector2ContinuousBar.TYPE);
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

        // Utils

        @ConfigSerializable
        public static final class NumberFormat {
            private static final NumberFormat instance = new NumberFormat();

            private final @Required String format;
            private final @Nullable MathNode expression;

            public NumberFormat(String format, @Nullable MathNode expression) {
                this.format = format;
                this.expression = expression;
            }

            private NumberFormat() {
                format = "";
                expression = null;
            }

            public String format() { return format; }
            public MathNode expression() { return expression; }

            public double express(double num) {
                return tryExpress(num, expression);
            }

            public double express(double num, Stat.Value<?> value) {
                if (value instanceof Primitives.AbstractNumber.FactorValue)
                    return num;
                return express(num);
            }

            public String format(Locale locale, double num) {
                try {
                    return String.format(locale, format, num);
                } catch (IllegalFormatException e) {
                    throw new FormatRenderException("Invalid format '" + format + "'", e);
                }
            }

            public String render(Locale locale, double num, Stat.Value<?> value) {
                return format(locale, express(num, value));
            }

            public String render(Locale locale, double num) {
                return format(locale, express(num));
            }
        }

        @ConfigSerializable
        public static final class BarOptions {
            private static final BarOptions instance = new BarOptions();

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

        // Formats

        @ConfigSerializable
        public static final class Raw extends AbstractFormat<Object> {
            public static final String ID = "raw";
            public static final FormatType TYPE = new FormatType(ID, Raw.class);

            private final boolean compute;
            private final boolean rendered;

            public Raw(String key, @Nullable String customLcKey, boolean compute, boolean rendered) {
                super(key, customLcKey);
                this.compute = compute;
                this.rendered = rendered;
            }

            public Raw() {
                compute = false;
                rendered = true;
            }

            @Override public String id() { return ID; }

            public boolean compute() { return compute; }
            public boolean rendered() { return rendered; }

            @Override
            public boolean accepts(Stat<?> stat) { return true; }

            @Override
            public <V> Component format(Locale locale, Localizer lc, Stat.Node<V> node) {
                return computeOrJoin(locale, lc, compute, node,
                        val -> rendered ? val.render(locale, lc) : text(val.toString()));
            }
        }

        @ConfigSerializable
        public static final class OfNumber extends AbstractFormat<Number> {
            public static final String ID = "number";
            public static final FormatType TYPE = new FormatType(ID, OfNumber.class);

            private final boolean compute;
            private final @Required NumberFormat format;

            public OfNumber(String key, @Nullable String customLcKey, boolean compute, NumberFormat format) {
                super(key, customLcKey);
                this.compute = compute;
                this.format = format;
            }

            public OfNumber() {
                compute = false;
                format = NumberFormat.instance;
            }

            @Override public String id() { return ID; }

            @Override
            public boolean accepts(Stat<?> stat) { return stat instanceof Primitives.SingleNumber; }

            @Override
            public <V extends Number> Component format(Locale locale, Localizer lc, Stat.Node<V> node) {
                return computeOrJoin(locale, lc, compute, node,
                        raw -> raw instanceof Primitives.SingleNumber.BaseValue<V> val
                                ? lc.safe(locale, lcKey(raw instanceof Primitives.SingleNumber.SetValue ? "set" : "op"),
                                "op", val.operator(),
                                "value", format.render(locale, val.wrappedValue().doubleValue(), val))
                                : null);
            }
        }

        @ConfigSerializable
        public static final class NumberBar extends AbstractFormat<Number> {
            public static final String ID = "number_bar";
            public static final FormatType TYPE = new FormatType(ID, NumberBar.class);

            private final @Required NumberFormat format;
            private final @Nullable MathNode barExpression;
            private final @Required BarOptions bar;
            private final @Required TextColor barColor;

            public NumberBar(String key, @Nullable String customLcKey, NumberFormat format, @Nullable MathNode barExpression, BarOptions bar, TextColor barColor) {
                super(key, customLcKey);
                this.format = format;
                this.barExpression = barExpression;
                this.bar = bar;
                this.barColor = barColor;
            }

            public NumberBar() {
                format = NumberFormat.instance;
                barExpression = null;
                bar = BarOptions.instance;
                barColor = NamedTextColor.WHITE;
            }

            @Override public String id() { return ID; }

            public NumberFormat format() { return format; }
            public MathNode barExpression() { return barExpression; }
            public BarOptions bar() { return bar; }
            public TextColor barColor() { return barColor; }

            @Override
            public boolean accepts(Stat<?> stat) { return stat instanceof Primitives.SingleNumber; }

            @Override
            public <V extends Number> Component format(Locale locale, Localizer lc, Stat.Node<V> node) {
                double val = node.compute().doubleValue();
                double barVal = tryExpress(val, barExpression);
                return lc.safe(locale, lcKey("format"),
                        "bar", bar.render(barSection(percent(val, bar.range), barColor)),
                        "value", format.render(locale, val),
                        "min", format.format(locale, bar.range.min()),
                        "max", format.format(locale, bar.range.max()));
            }
        }

        public static abstract class AbstractVector2 extends AbstractFormat<Vector2> {
            public AbstractVector2(String key, @Nullable String customLcKey) {
                super(key, customLcKey);
            }

            public AbstractVector2() {}

            @Override
            public boolean accepts(Stat<?> stat) { return stat instanceof Vectors.OfVector2; }

            @Contract("null, _ -> false")
            protected boolean merged(@Nullable Object config, Vector2 val) {
                return config != null && Double.compare(val.x(), val.y()) == 0;
            }
        }

        @ConfigSerializable
        public static final class OfVector2 extends AbstractVector2 {
            public static final String ID = "vector2";
            public static final FormatType TYPE = new FormatType(ID, OfVector2.class);

            @ConfigSerializable
            public record ComponentConfig(
                    @Required NumberFormat format
            ) {
                private static final ComponentConfig instance = new ComponentConfig();

                public ComponentConfig() {
                    this(NumberFormat.instance);
                }
            }

            private final boolean compute;
            private final @Required ComponentConfig x;
            private final @Required ComponentConfig y;
            private final @Nullable ComponentConfig merged;

            public OfVector2(String key, @Nullable String customLcKey, boolean compute, ComponentConfig x, ComponentConfig y, @Nullable ComponentConfig merged) {
                super(key, customLcKey);
                this.compute = compute;
                this.x = x;
                this.y = y;
                this.merged = merged;
            }

            public OfVector2() {
                compute = false;
                x = ComponentConfig.instance;
                y = ComponentConfig.instance;
                merged = null;
            }

            @Override public String id() { return ID; }

            public boolean compute() { return compute; }
            public ComponentConfig x() { return x; }
            public ComponentConfig y() { return y; }
            public ComponentConfig merged() { return merged; }

            @Override
            public <V extends Vector2> Component format(Locale locale, Localizer lc, Stat.Node<V> node) {
                return computeOrJoin(locale, lc, compute, node,
                        raw -> raw instanceof Vectors.OfVector.BaseValue<V> val ? merged(merged, val.value())
                                ? lc.safe(locale, lcKey("merged." + (raw instanceof Vectors.OfVector.SetValue ? "set" : "op")),
                                        "op", val.operator(),
                                        "value", merged.format.render(locale, val.value().x(), raw))
                                : lc.safe(locale, lcKey("split." + (raw instanceof Vectors.OfVector.SetValue ? "set" : "op")),
                                        "op", val.operator(),
                                        "x", x.format.render(locale, val.value().x(), raw),
                                        "y", y.format.render(locale, val.value().y(), raw))
                                        : null,
                        val -> merged(merged, val)
                                ? lc.safe(locale, lcKey("merged.compute"),
                                        "value", merged.format.format(locale, val.x()))
                                : lc.safe(locale, lcKey("split.compute"),
                                        "x", x.format.format(locale, val.x()),
                                        "y", y.format.format(locale, val.y())));
            }
        }

        @ConfigSerializable
        public static final class Vector2SeparateBars extends AbstractVector2 {
            public static final String ID = "vector2_separate_bars";
            public static final FormatType TYPE = new FormatType(ID, Vector2SeparateBars.class);

            @ConfigSerializable
            public record ComponentConfig(
                    @Required NumberFormat format,
                    @Nullable MathNode barExpression,
                    @Required BarOptions bar,
                    @Required TextColor barColor
            ) {
                private static final ComponentConfig instance = new ComponentConfig();

                public ComponentConfig() {
                    this(NumberFormat.instance, null, BarOptions.instance, NamedTextColor.WHITE);
                }
            }

            private final @Required ComponentConfig x;
            private final @Required ComponentConfig y;
            private final @Nullable ComponentConfig merged;

            public Vector2SeparateBars(String key, @Nullable String customLcKey, ComponentConfig x, ComponentConfig y, @Nullable ComponentConfig merged) {
                super(key, customLcKey);
                this.x = x;
                this.y = y;
                this.merged = merged;
            }

            public Vector2SeparateBars() {
                x = ComponentConfig.instance;
                y = ComponentConfig.instance;
                merged = ComponentConfig.instance;
            }

            @Override public String id() { return ID; }

            public ComponentConfig x() { return x; }
            public ComponentConfig y() { return y; }
            public ComponentConfig merged() { return merged; }

            @Override
            public <V extends Vector2> Component format(Locale locale, Localizer lc, Stat.Node<V> node) {
                Vector2 val = node.compute();
                Vector2 barVal = Vector2.vec2(
                        tryExpress(val.x(), x.barExpression),
                        tryExpress(val.y(), y.barExpression)
                );
                val = Vector2.vec2(
                        x.format.express(val.x()),
                        y.format.express(val.y())
                );
                return merged(merged, val)
                        ? lc.safe(locale, lcKey("merged.format"),
                                "bar", merged.bar.render(barSection(percent(barVal.x(), merged.bar.range), merged.barColor)),
                                "bar_x", x.bar.render(barSection(percent(barVal.x(), x.bar.range), x.barColor)),
                                "bar_y", y.bar.render(barSection(percent(barVal.y(), y.bar.range), y.barColor)),
                                "value", merged.format.format(locale, val.x()),
                                "min_x", x.format.format(locale, x.bar.range.min()),
                                "max_x", x.format.format(locale, x.bar.range.max()),
                                "min_y", x.format.format(locale, y.bar.range.min()),
                                "max_y", x.format.format(locale, y.bar.range.max()))
                        : lc.safe(locale, lcKey("split.format"),
                                "bar_x", x.bar.render(barSection(percent(barVal.x(), x.bar.range), x.barColor)),
                                "bar_y", y.bar.render(barSection(percent(barVal.y(), y.bar.range), y.barColor)),
                                "x", x.format.format(locale, val.x()),
                                "y", y.format.format(locale, val.y()),
                                "min_x", x.format.format(locale, x.bar.range.min()),
                                "max_x", x.format.format(locale, x.bar.range.max()),
                                "min_y", x.format.format(locale, y.bar.range.min()),
                                "max_y", x.format.format(locale, y.bar.range.max()));
            }
        }

        @ConfigSerializable
        public static final class Vector2ContinuousBar extends AbstractVector2 {
            public static final String ID = "vector2_continuous_bar";
            public static final FormatType TYPE = new FormatType(ID, Vector2ContinuousBar.class);

            @ConfigSerializable
            public record ComponentConfig(
                    @Required NumberFormat format,
                    @Nullable MathNode barExpression,
                    @Required TextColor barColor
            ) {
                private static final ComponentConfig instance = new ComponentConfig();

                public ComponentConfig() {
                    this(NumberFormat.instance, null, NamedTextColor.WHITE);
                }
            }

            private final @Required int[] barOrder;
            private final @Required BarOptions bar;
            private final @Required ComponentConfig x;
            private final @Required ComponentConfig y;
            private final @Nullable ComponentConfig merged;

            public Vector2ContinuousBar(String key, @Nullable String customLcKey, int[] barOrder, BarOptions bar, ComponentConfig x, ComponentConfig y, ComponentConfig merged) {
                super(key, customLcKey);
                this.barOrder = barOrder;
                this.bar = bar;
                this.x = x;
                this.y = y;
                this.merged = merged;
            }

            public Vector2ContinuousBar() {
                barOrder = new int[0];
                bar = BarOptions.instance;
                x = ComponentConfig.instance;
                y = ComponentConfig.instance;
                merged = ComponentConfig.instance;
            }

            @Override
            public void validate() throws FormatValidationException {
                if (barOrder.length != 3)
                    throw new FormatValidationException("Bar order must be array of 3 numbers");
            }

            @Override public String id() { return ID; }

            public int[] barOrder() { return barOrder; }
            public BarOptions bar() { return bar; }
            public ComponentConfig x() { return x; }
            public ComponentConfig y() { return y; }
            public ComponentConfig merged() { return merged; }

            @Override
            public <V extends Vector2> Component format(Locale locale, Localizer lc, Stat.Node<V> node) {
                Vector2 val = node.compute();
                Vector2 barVal = Vector2.vec2(
                        tryExpress(val.x(), x.barExpression),
                        tryExpress(val.y(), y.barExpression)
                );
                val = Vector2.vec2(
                        x.format.express(val.x()),
                        y.format.express(val.y())
                );

                double bar1 = percent(barVal.x(), bar.range);
                double bar2 = percent(barVal.y(), bar.range);

                Components.BarSection[] sections = new Components.BarSection[] {
                        barSection(1 - bar1 - bar2, bar.fill),
                        barSection(bar1, x.barColor),
                        barSection(bar2, y.barColor)
                };
                return merged(merged, val)
                        ? lc.safe(locale, lcKey("merged.format"),
                                "bar", Components.bar(bar.length, bar.placeholder, sections[barOrder[2]].color(),
                                        sections[barOrder[0]],
                                        sections[barOrder[1]]),
                                "value", merged.format.format(locale, val.x()),
                                "min", x.format.format(locale, bar.range.min()),
                                "max", x.format.format(locale, bar.range.max()))
                        : lc.safe(locale, lcKey("split.format"),
                                "bar", Components.bar(bar.length, bar.placeholder, sections[barOrder[2]].color(),
                                        sections[barOrder[0]],
                                        sections[barOrder[1]]),
                                "x", x.format.format(locale, val.x()),
                                "y", y.format.format(locale, val.y()),
                                "min", x.format.format(locale, bar.range.min()),
                                "max", x.format.format(locale, bar.range.max()));
            }
        }
    }
}
