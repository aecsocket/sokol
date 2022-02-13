package com.github.aecsocket.sokol.core.stat;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.i18n.Renderable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.*;

public abstract class Stat<T> implements Renderable {
    public static final String
        STAT = "stat",
        NAME = "name",
        FORMAT = "format";

    public interface Op<T> extends Renderable {
        T compute(T cur);

        interface Discards {}

        interface Initial<T> extends Op<T> {
            T first();

            @Override
            default T compute(T cur) {
                return first();
            }
        }
    }

    @FunctionalInterface
    public interface OpFunction<T> {
        Op<T> create(Type type, ConfigurationNode node, ConfigurationNode... args) throws SerializationException;
    }

    public record OpType<T>(
        String name,
        OpFunction<T> function,
        String[] args
    ) implements OpFunction<T> {
        @Override
        public Op<T> create(Type type, ConfigurationNode node, ConfigurationNode... args) throws SerializationException {
            return function.create(type, node, args);
        }
    }

    public interface OpTypes<T> extends Map<String, OpType<T>> {
        Optional<OpType<T>> defaultOp();
    }

    private static final class OpTypesImpl<T> extends HashMap<String, OpType<T>> implements OpTypes<T> {
        private final @Nullable OpType<T> def;

        public OpTypesImpl(Map<? extends String, ? extends OpType<T>> m, @Nullable OpType<T> def) {
            super(m);
            this.def = def;
        }

        @Override public Optional<OpType<T>> defaultOp() { return Optional.ofNullable(def); }
    }

    public static final class OpTypesBuilder<T> {
        private final Map<String, OpType<T>> types = new HashMap<>();
        private @Nullable OpType<T> def;

        private OpTypesBuilder() {}

        public OpTypesBuilder<T> set(String name, OpFunction<T> function, String... args) {
            types.put(name, new OpType<>(name, function, args));
            return this;
        }

        public OpTypesBuilder<T> setDefault(String name, OpFunction<T> function, String... args) {
            OpType<T> type = new OpType<>(name, function, args);
            types.put(name, type);
            def = type;
            return this;
        }

        public OpTypes<T> build() {
            return new OpTypesImpl<>(Collections.unmodifiableMap(types), def);
        }
    }

    public static <T> OpTypesBuilder<T> buildOpTypes() {
        return new OpTypesBuilder<>();
    }

    public static final class Node<T> implements Iterable<Node<T>>, Renderable {
        public static final String STAT_NODE_SEPARATOR = "stat_node.separator";

        private final Stat<T> stat;
        private final Stat.Op<T> op;
        private @Nullable Node<T> next;

        public Node(Stat<T> stat, Stat.Op<T> op, @Nullable Node<T> next) {
            this.stat = stat;
            this.op = op;
            this.next = next;
        }

        public Node(Stat<T> stat, Stat.Op<T> op) {
            this.stat = stat;
            this.op = op;
        }

        public Stat<T> stat() { return stat; }
        public Stat.Op<T> op() { return op; }

        public Node<T> next() { return next; }
        public void next(Node<T> next) { this.next = next; }

        public Node<T> chain(Node<T> next) {
            Node<T> cur = this;
            for (; cur.next != null; cur = cur.next);
            cur.next = next;
            return cur;
        }

        public T compute() {
            if (!(op instanceof Stat.Op.Initial<T> initial))
                throw new IllegalStateException("Stat chain must start with an initial operation");
            T value = initial.first();
            for (Node<T> cur = next; cur != null; cur = cur.next) {
                value = cur.op.compute(value);
            }
            return value;
        }

        public Node<T> copy() {
            return new Node<>(stat, op, next == null ? null : next.copy());
        }

        @Override
        public Component render(I18N i18n, Locale locale) {
            List<Component> result = new ArrayList<>();
            for (Node<T> cur = this; cur != null; cur = cur.next) {
                result.add(cur.op.render(i18n, locale));
            }
            return Component.join(JoinConfiguration.separator(i18n.line(locale, STAT_NODE_SEPARATOR)), result);
        }

        public int size() {
            int result = 0;
            for (Node<T> cur = this; cur != null; cur = cur.next) {
                ++result;
            }
            return result;
        }

        public List<Node<T>> asList() {
            List<Node<T>> result = new LinkedList<>();
            for (Node<T> cur = this; cur != null; cur = cur.next) {
                result.add(cur);
            }
            return result;
        }

        @NotNull
        @Override
        public Iterator<Node<T>> iterator() {
            return new Iterator<Node<T>>() {
                Node<T> cur = Node.this;

                @Override public boolean hasNext() {return cur != null; }

                @Override
                public Node<T> next() {
                    Node<T> ret = cur;
                    cur = cur.next;
                    return ret;
                }
            };
        }

        @Override
        public String toString() {
            return op + (next == null ? "" : " -> " + next);
        }
    }

    protected final String key;
    protected final @Nullable T def;

    protected Stat(String key, @Nullable T def) {
        this.key = key;
        this.def = def;
    }

    public String key() { return key; }
    public Optional<T> defaultValue() { return Optional.ofNullable(def); }

    public Node<T> node(Op<T> op) {
        return new Node<>(this, op);
    }

    @Override
    public final Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, STAT + "." + key + "." + NAME);
    }

    public Optional<Component> renderFormat(I18N i18n, Locale locale, Component value) {
        return i18n.orLine(locale, STAT + "." + key + "." + FORMAT,
            c -> c.of("value", () -> value));
    }

    public abstract Component renderValue(I18N i18n, Locale locale, T value);

    public abstract OpTypes<T> opTypes();
}
