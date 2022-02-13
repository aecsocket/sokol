package com.github.aecsocket.sokol.core.feature;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.registry.Registry;
import com.github.aecsocket.sokol.core.rule.RuleTypes;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatMap;
import com.github.aecsocket.sokol.core.stat.StatTypes;
import com.github.aecsocket.sokol.core.world.ItemCreationException;
import com.github.aecsocket.sokol.core.world.ItemStack;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.util.*;

import static net.kyori.adventure.text.Component.text;

public abstract class StatDisplay<
    F extends StatDisplay<F, P, D, I, N, S>,
    P extends StatDisplay<F, P, D, I, N, S>.Profile,
    D extends StatDisplay<F, P, D, I, N, S>.Profile.Data,
    I extends StatDisplay<F, P, D, I, N, S>.Profile.Data.Instance,
    N extends TreeNode.Scoped<N, ?, ?, ?, S>,
    S extends ItemStack.Scoped<S, ?>
> implements Feature<P> {
    public static final String
        ID = "stat_display",
        KEY_SEPARATOR = "feature." + ID + ".separator",
        KEY_ENTRY = "feature." + ID + ".entry";

    public interface Format<T> {
        String id();
        String key();
        boolean accepts(Stat<?> stat);
        <V extends T> Component render(I18N i18n, Locale locale, Stat.Node<V> node) throws FormatRenderException;
        default void validate() throws FormatValidationException {}

        record Type(String id, Class<? extends Format<?>> type) implements Keyed {
            public static final String I18N_KEY = "stat_format";

            @Override public String i18nBase() { return I18N_KEY; }

            public static void registerDefaults(Registry<Type> registry) {
                registry.register(Raw.TYPE);
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

    public static class FormatRenderException extends Exception {
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
        protected final List<List<Format<?>>> sections;
        protected final String padding;
        protected final int paddingWidth;

        public Profile(int listenerPriority, List<List<Format<?>>> sections, String padding, int paddingWidth) {
            this.listenerPriority = listenerPriority;
            this.sections = sections;
            this.padding = padding;
            this.paddingWidth = paddingWidth;
        }

        public int listenerPriority() { return listenerPriority; }
        public List<List<Format<?>>> sections() { return sections; }
        public String padding() { return padding; }
        public int paddingWidth() { return paddingWidth; }

        protected abstract P self();
        @Override public F type() { return StatDisplay.this.self(); }

        public abstract class Data implements FeatureData<P, I, N> {
            protected abstract D self();
            @Override public P profile() { return Profile.this.self(); }

            @Override public void save(ConfigurationNode node) throws SerializationException {}

            protected abstract int width(String text);

            public abstract class Instance implements FeatureInstance<D, N> {
                @Override public D asData() { return self(); }

                @Override
                public void build(Tree<N> tree, N parent, StatIntermediate stats) {
                    if (parent.isRoot()) {
                        tree.events().register(new TypeToken<NodeEvent.CreateItem<N, ?, S>>() {}, this::onEvent);
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
                        for (var format : section) {
                            String key = format.key();
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
                            secLines.addAll(lines(locale, format, stats, keyData.get(format.key()), longest));
                        }
                        if (secLines.size() > 0)
                            rendered.add(secLines);
                    }

                    List<Component> separator = i18n.lines(locale, KEY_SEPARATOR);
                    for (int i = 0; i < rendered.size(); i++) {
                        if (i > 0)
                            lines.addAll(separator);
                        lines.addAll(rendered.get(i));
                    }
                }

                protected <T> List<Component> lines(Locale locale, Format<T> format, StatMap stats, KeyData keyData, int longest) {
                    String key = format.key();
                    Stat.Node<?> rawNode = stats.get(key);
                    if (rawNode == null)
                        return Collections.emptyList();
                    if (!format.accepts(rawNode.stat()))
                        throw new ItemCreationException("Format `" + format.id() + "` does not accept stat `" + key + "` of type `" + rawNode.stat().getClass().getName() + "`");
                    @SuppressWarnings("unchecked")
                    Stat.Node<T> node = (Stat.Node<T>) rawNode;

                    Component rendered;
                    try {
                        rendered = format.render(i18n, locale, node);
                    } catch (FormatRenderException e) {
                        throw new ItemCreationException("Could not render stat `" + key + "`", e);
                    }
                    return i18n.lines(locale, KEY_ENTRY,
                        c -> c.of("padding", () -> text(padding.repeat((longest - keyData.width) / paddingWidth))),
                        c -> c.of("key", () -> text(key)),
                        c -> c.of("stat", () -> keyData.component),
                        c -> c.of("value", () -> rendered));
                }
            }
        }
    }

    @ConfigSerializable
    public record Raw(
        @Required String key, @Nullable String i18nKey, boolean useToString
    ) implements Format<Object> {
        public static final String ID = "raw";
        public static final Format.Type TYPE = new Format.Type(ID, Raw.class);

        public Raw() {
            this(null, null, false);
        }

        @Override public String id() { return ID; }
        @Override public boolean accepts(Stat<?> stat) { return true; }

        @Override
        public <V> Component render(I18N i18n, Locale locale, Stat.Node<V> node) throws FormatRenderException {
            V value = node.compute();
            return useToString ? text(value.toString()) : node.stat().renderValue(i18n, locale, value);
        }
    }
}
