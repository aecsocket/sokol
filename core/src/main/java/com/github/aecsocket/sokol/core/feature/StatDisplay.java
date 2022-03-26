package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.minecommons.core.Components;
import com.github.aecsocket.minecommons.core.Numbers;
import com.github.aecsocket.minecommons.core.Range;
import com.github.aecsocket.minecommons.core.expressions.math.MathNode;
import com.github.aecsocket.minecommons.core.expressions.node.EvaluationException;
import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.impl.AbstractFeatureInstance;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatMap;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.github.aecsocket.sokol.core.stat.impl.PrimitiveStat;
import com.github.aecsocket.sokol.core.world.ItemCreationException;
import com.github.aecsocket.sokol.core.world.ItemStack;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.util.*;
import java.util.function.Supplier;

import static net.kyori.adventure.text.Component.text;
import static com.github.aecsocket.minecommons.core.Components.barSection;

public abstract class StatDisplay<
    F extends StatDisplay<F, P, D, I, N, S>,
    P extends StatDisplay<F, P, D, I, N, S>.Profile,
    D extends StatDisplay<F, P, D, I, N, S>.Profile.Data,
    I extends StatDisplay<F, P, D, I, N, S>.Profile.Instance,
    N extends TreeNode.Scoped<N, ?, ?, ?, S>,
    S extends ItemStack.Scoped<S, ?>
> implements Feature<P> {
    public static final String
        ID = "stat_display",
        KEY_SECTION_SEPARATOR = "feature." + ID + ".section_separator",
        KEY_FORMAT_SEPARATOR = "feature." + ID + ".format_separator",
        KEY_NODE_SEPARATOR = "feature." + ID + ".node_separator",
        KEY_ENTRY = "feature." + ID + ".entry",
        STAT_FORMAT = "stat_format",
        VALUE = "value";

    public interface Format<T> {
        String id();
        @Nullable String i18nKey();
        boolean accepts(Stat<?> stat);
        default void validate() throws FormatValidationException {}
        <V extends T> Component render(I18N i18n, Locale locale, Supplier<V> computed, Stat.Node<V> node) throws FormatRenderException;

        default String i18n(String key) {
            return STAT_FORMAT + "." + (i18nKey() == null ? id() : i18nKey()) + "." + key;
        }

        static double tryExpress(double value, @Nullable MathNode expression) {
            if (expression == null)
                return value;
            try {
                return expression.set("x", value).eval();
            } catch (EvaluationException e) {
                throw new IllegalArgumentException("Could not evaluate expression", e);
            }
        }

        static double percent(double value, Range.Double range) {
            return Numbers.clamp01((value - range.min()) / (range.max() - range.min()));
        }

        record Type(String id, Class<? extends Format<?>> type) implements Keyed {
            @Override public String i18nBase() { return STAT_FORMAT; }

            public static void registerDefaults(Registry<Type> registry) {
                registry.register(Raw.TYPE);
                registry.register(OfNumber.TYPE);
                registry.register(NumberBar.TYPE);
                registry.register(NumberChangeBar.TYPE);
            }
        }

        final class Serializer implements TypeSerializer<Format<?>> {
            public static final String TYPE = "type";

            private final Registry<Type> registry;

            public Serializer(Registry<Type> registry) {
                this.registry = registry;
            }

            @Override
            public void serialize(java.lang.reflect.Type type, @Nullable Format<?> obj, ConfigurationNode node) throws SerializationException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Format<?> deserialize(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
                String id = Serializers.require(node.node(TYPE), String.class);
                Format<?> format = node.get(registry.get(id)
                    .orElseThrow(() -> new SerializationException(node, type, "Invalid stat format type `" + id + "`"))
                    .type);
                if (format == null)
                    throw new SerializationException(node, type, "Null format deserialized");
                if (!id.equals(format.id()))
                    throw new SerializationException(node, type, "ID mismatch: type: `" + id + "` / format: `" + format.id() + "`");
                try {
                    format.validate();
                } catch (FormatValidationException e) {
                    throw new SerializationException(node, type, e);
                }
                return format;
            }
        }
    }

    public static class FormatRenderException extends RuntimeException {
        public FormatRenderException() {}

        public FormatRenderException(String message) {
            super(message);
        }

        public FormatRenderException(String message, Throwable cause) {
            super(message, cause);
        }

        public FormatRenderException(Throwable cause) {
            super(cause);
        }
    }

    public static class FormatValidationException extends Exception {
        public FormatValidationException() {}

        public FormatValidationException(String message) {
            super(message);
        }

        public FormatValidationException(String message, Throwable cause) {
            super(message, cause);
        }

        public FormatValidationException(Throwable cause) {
            super(cause);
        }
    }

    @ConfigSerializable
    public record FormatChain<T>(
        @Required String key,
        @Required List<Format<T>> formats
    ) {}

    protected final I18N i18n;

    public StatDisplay(I18N i18n) {
        this.i18n = i18n;
    }

    protected abstract F self();
    protected abstract SokolPlatform platform();

    @Override public StatTypes statTypes() { return StatTypes.empty(); }
    @Override public RuleTypes ruleTypes() { return RuleTypes.empty(); }
    @Override public final String id() { return ID; }

    public abstract class Profile implements FeatureProfile<F, D> {
        protected final int listenerPriority;
        protected final List<List<FormatChain<?>>> sections;
        protected final String padding;
        protected final int paddingWidth;

        public Profile(int listenerPriority, List<List<FormatChain<?>>> sections, String padding, int paddingWidth) {
            this.listenerPriority = listenerPriority;
            this.sections = sections;
            this.padding = padding;
            this.paddingWidth = paddingWidth;
        }

        public int listenerPriority() { return listenerPriority; }
        public List<List<FormatChain<?>>> sections() { return sections; }
        public String padding() { return padding; }
        public int paddingWidth() { return paddingWidth; }

        protected abstract P self();
        @Override public F type() { return StatDisplay.this.self(); }

        @Override public void validate(SokolComponent parent) throws FeatureValidationException {}

        protected abstract int width(String text);

        public abstract class Data implements FeatureData<P, I, N> {
            @Override public P profile() { return self(); }

            @Override public void save(ConfigurationNode node) throws SerializationException {}
        }

        public abstract class Instance extends AbstractFeatureInstance<P, D, N> {
            @Override public P profile() { return self(); }

            @Override
            public void build(Tree<N> tree, N parent, StatIntermediate stats) {
                super.build(tree, parent, stats);
                if (parent.isRoot()) {
                    tree.events().register(new TypeToken<NodeEvent.CreateItem<N, ?, S>>() {}, this::onEvent, listenerPriority);
                }
            }

            protected void onEvent(NodeEvent.CreateItem<N, ?, S> event) {
                N node = event.node();
                S item = event.item();
                Locale locale = node.context().locale();
                List<Component> lines = new ArrayList<>();

                lines(lines, locale, node.tree().stats());

                item.addLore(locale, lines);
            }

            private record KeyData(Component component, int width) {}

            protected void lines(List<Component> lines, Locale locale, StatMap stats) {
                Map<String, KeyData> keyData = new HashMap<>();
                int longest = 0;
                for (var section : sections) {
                    for (var formats : section) {
                        String key = formats.key();
                        if (keyData.containsKey(key))
                            continue;
                        Stat.Node<?> node = stats.get(key);
                        if (node == null)
                            continue;
                        Component component = node.stat().render(i18n, locale);
                        int width = width(PlainTextComponentSerializer.plainText().serialize(component));
                        keyData.put(key, new KeyData(component, width));
                        if (width > longest)
                            longest = width;
                    }
                }

                List<List<Component>> rendered = new ArrayList<>();
                for (var section : sections) {
                    List<Component> secLines = new ArrayList<>();
                    for (var format : section) {
                        secLines.addAll(lines(locale, format, stats, keyData.get(format.key), longest));
                    }
                    if (secLines.size() > 0)
                        rendered.add(secLines);
                }

                List<Component> separator = i18n.lines(locale, KEY_SECTION_SEPARATOR);
                for (int i = 0; i < rendered.size(); i++) {
                    if (i > 0)
                        lines.addAll(separator);
                    lines.addAll(rendered.get(i));
                }
            }

            protected <T> List<Component> lines(Locale locale, FormatChain<T> formats, StatMap stats, KeyData keyData, int longest) {
                String key = formats.key();
                Stat.Node<?> rawNode = stats.get(key);
                if (rawNode == null)
                    return Collections.emptyList();
                // TODO we have to check if this is a safe cast. Do this at validation time somehow?
                // also remove the old method for validation on a format
                @SuppressWarnings("unchecked")
                Stat.Node<T> node = (Stat.Node<T>) rawNode;

                Supplier<T> value = new Supplier<>() {
                    T computed;

                    @Override
                    public T get() {
                        if (computed == null)
                            computed = node.compute();
                        return computed;
                    }
                };

                List<Component> rdFormats = new ArrayList<>();
                for (var format : formats.formats) {
                    try {
                        Component rendered = format.render(i18n, locale, value, node);
                        rdFormats.add(i18n.orLine(locale, format.i18n(VALUE),
                                        c -> c.of("value", () -> rendered))
                                .orElse(rendered));
                    } catch (FormatRenderException e) {
                        throw new ItemCreationException("Could not render stat `" + key + "`", e);
                    }
                }
                Component rendered = Component.join(JoinConfiguration.separator(i18n.line(locale, KEY_FORMAT_SEPARATOR)), rdFormats);
                return i18n.lines(locale, KEY_ENTRY,
                        c -> c.of("padding", () -> text(padding.repeat((longest - keyData.width) / paddingWidth))),
                        c -> c.of("key", () -> text(key)),
                        c -> c.of("stat", () -> keyData.component),
                        c -> c.of("value", () -> rawNode.stat().renderFormat(i18n, locale, rendered)
                                .orElse(rendered)));
            }
        }
    }

    // Utils

    @ConfigSerializable
    public record NumberOptions(
        @Required String format,
        @Nullable MathNode expression,
        Direction better
    ) {
        public enum Direction {
            HIGHER, LOWER, NONE
        }

        public NumberOptions() {
            this(null, null, Direction.NONE);
        }

        public String format(Locale locale, double value) throws FormatRenderException {
            try {
                return String.format(locale, format, value);
            } catch (IllegalFormatException e) {
                throw new FormatRenderException("Invalid format `" + format + "`", e);
            }
        }

        public double express(double value) {
            return Format.tryExpress(value, expression);
        }

        public Component renderOp(Format<?> format, I18N i18n, Locale locale, double value, PrimitiveStat.OfNumber.NumberOp<?> op, Direction direction) {
            value = express(value);
            double fValue = value;
            return i18n.line(locale, format.i18n(
                (better == null || direction == Direction.NONE ? "neutral"
                    : direction == better ? "better" : "worse") +
                    "." + op.name()),
                c -> c.of("value", () -> text(fValue) /* todo percents? */));
        }

        public Component renderResult(Format<?> format, I18N i18n, Locale locale, double value) {
            value = express(value);
            double fValue = value;
            return i18n.line(locale, format.i18n("result"),
                c -> c.of("value", () -> text(fValue)));
        }

        public static Direction direction(double num, PrimitiveStat.OfNumber.NumberOp<?> op) {
            if (op instanceof PrimitiveStat.OfNumber.SumOp) {
                num = op instanceof PrimitiveStat.OfNumber.SubtractOp ? -num : num;
                return num > 0 ? Direction.HIGHER : num < 0 ? Direction.LOWER : Direction.NONE;
            }
            if (op instanceof PrimitiveStat.OfNumber.FactorOp) {
                num = op instanceof PrimitiveStat.OfNumber.DivideOp<?> ? 1 / num : num;
                return num > 1 ? Direction.HIGHER : num < 1 ? Direction.LOWER : Direction.NONE;
            }
            return Direction.NONE;
        }
    }

    @ConfigSerializable
    public record BarOptions(
        @Required int length,
        @Required TextColor fill,
        String placeholder
    ) {
        public BarOptions() {
            this(0, null, " ");
        }

        public Component render(Components.BarSection... sections) {
            return Components.bar(length, placeholder, fill, sections);
        }

        public Component simple(double value, TextColor color) {
            return render(barSection(value, color));
        }

        public Component change(double before, double after, TextColor color, TextColor higher, TextColor lower) {
            if (before > after) {
                return render(barSection(Math.max(0, after), color), barSection(before - after, lower));
            } else if (after > before) {
                return render(barSection(Math.max(0, before), color), barSection(after - before, higher));
            } else
                return simple(before, color);
        }
    }

    // Formats

    @ConfigSerializable
    public record Raw(
        @Nullable String i18nKey,
        boolean useToString
    ) implements Format<Object> {
        public static final String ID = "raw";
        public static final Format.Type TYPE = new Format.Type(ID, Raw.class);

        public Raw() {
            this(null, false);
        }

        @Override public String id() { return ID; }
        @Override public boolean accepts(Stat<?> stat) { return true; }

        @Override
        public <V> Component render(I18N i18n, Locale locale, Supplier<V> computed, Stat.Node<V> node) throws FormatRenderException {
            return useToString ? text(computed.get().toString()) : node.stat().renderValue(i18n, locale, computed.get());
        }
    }

    @ConfigSerializable
    public record OfNumber(
        @Nullable String i18nKey,
        @Required NumberOptions options,
        boolean useComputed
    ) implements Format<Number> {
        public static final String ID = "number";
        public static final Format.Type TYPE = new Format.Type(ID, OfNumber.class);

        public OfNumber() {
            this(null, null, false);
        }

        @Override public String id() { return ID; }
        @Override public boolean accepts(Stat<?> stat) { return stat instanceof PrimitiveStat.OfNumber; }

        @Override
        public <V extends Number> Component render(I18N i18n, Locale locale, Supplier<V> computed, Stat.Node<V> node) throws FormatRenderException {
            if (useComputed) {
                double value = computed.get().doubleValue();
                return options.renderResult(this, i18n, locale, value);
            } else {
                List<Component> rendered = new ArrayList<>();
                for (var cur = node; cur != null; cur = cur.next()) {
                    if (!(cur.op() instanceof PrimitiveStat.OfNumber.NumberOp op))
                        throw new IllegalStateException("Encountered op of type `" + cur.op().getClass() + "` when rendering number format");
                    double value = op.asDouble();
                    rendered.add(options.renderOp(this, i18n, locale, value, op, NumberOptions.direction(value, op)));
                }
                return Component.join(JoinConfiguration.separator(i18n.line(locale, i18n("separator"))), rendered);
            }
        }
    }

    @ConfigSerializable
    public record NumberBar(
        @Nullable String i18nKey,
        @Required BarOptions bar,
        @Required Range.Double range,
        @Required TextColor color,
        @Nullable MathNode expression
    ) implements Format<Number> {
        public static final String ID = "number_bar";
        public static final Format.Type TYPE = new Format.Type(ID, NumberBar.class);

        public NumberBar() {
            this(null, null, null, null, null);
        }

        @Override public String id() { return ID; }
        @Override public boolean accepts(Stat<?> stat) { return stat instanceof PrimitiveStat.OfNumber; }

        @Override
        public <V extends Number> Component render(I18N i18n, Locale locale, Supplier<V> computed, Stat.Node<V> node) throws FormatRenderException {
            double value = Format.tryExpress(computed.get().doubleValue(), expression);
            return bar.simple(Format.percent(value, range), color);
        }
    }

    @ConfigSerializable
    public record NumberChangeBar(
        @Nullable String i18nKey,
        @Required BarOptions bar,
        @Required Range.Double range,
        @Required TextColor color,
        @Required TextColor higher,
        @Required TextColor lower,
        @Nullable MathNode expression
    ) implements Format<Number> {
        public static final String ID = "number_change_bar";
        public static final Format.Type TYPE = new Format.Type(ID, NumberChangeBar.class);

        public NumberChangeBar() {
            this(null, null, null, null, null, null, null);
        }

        @Override public String id() { return ID; }
        @Override public boolean accepts(Stat<?> stat) { return stat instanceof PrimitiveStat.OfNumber; }

        @Override
        public <V extends Number> Component render(I18N i18n, Locale locale, Supplier<V> computed, Stat.Node<V> node) throws FormatRenderException {
            double after = Format.tryExpress(computed.get().doubleValue(), expression);
            if (node.op() instanceof PrimitiveStat.OfNumber.SetOp<V> op) {
                double before = Format.tryExpress(op.asDouble(), expression);
                return bar.change(Format.percent(before, range), Format.percent(after, range), color, higher, lower);
            } else
                return bar.simple(Format.percent(after, range), color);
        }
    }
}
