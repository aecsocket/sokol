package com.gitlab.aecsocket.sokol.core.stat.collection;

import com.gitlab.aecsocket.minecommons.core.Validation;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A map of string names to stat nodes.
 */
public class StatMap extends HashMap<String, Stat.Node<?>> {
    /**
     * The priority of a map, determining when it should be applied in a tree node's build process.
     */
    public record Priority(int value, boolean reverse) {
        /**
         * The default priority of {@code 0}.
         */
        public static final Priority DEFAULT = new Priority(0, false);

        public Priority {
            Validation.greaterThanEquals("value", value, 0);
        }

        @Override
        public String toString() {
            return reverse ? "[" + value + "]" : Integer.toString(value);
        }

        /**
         * Type serializer for a {@link Priority}.
         * <p>
         * Positive value: {@code priority}
         * <p>
         * Negative value: {@code [priority]}
         */
        public static final class Serializer implements TypeSerializer<Priority> {
            @Override
            public void serialize(Type type, @Nullable Priority obj, ConfigurationNode node) throws SerializationException {
                if (obj == null) node.set(null);
                else {
                    if (obj.reverse)
                        node.setList(Integer.class, Collections.singletonList(obj.value));
                    else
                        node.set(obj.value);
                }
            }

            @Override
            public Priority deserialize(Type type, ConfigurationNode node) throws SerializationException {
                if (node.isList())
                    return new Priority(node.node(0).getInt(), true);
                return new Priority(node.getInt(), false);
            }
        }
    }

    /**
     * Serializer for a {@link StatMap}.
     * <p>
     * For deserialization, requires the base stat types to be specified using {@link #types(StatTypes)}.
     */
    public static final class Serializer implements TypeSerializer<StatMap> {
        public static final String KEY_OP = "op";
        public static final String KEY_ARGS = "args";

        private @Nullable StatTypes types;

        public @Nullable StatTypes types() { return types; }
        public void types(@Nullable StatTypes types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable StatMap obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(obj.toTypes());
                node.node("priority").set(obj.priority);
            }
        }

        private <T> void add(Type type, String key, ConfigurationNode node, Stat<T> stat, StatMap result) throws SerializationException {
            List<Stat.Node<T>> chain = new ArrayList<>();
            for (ConfigurationNode child : node.isList() && node.childrenList().size() > 0 ? node.childrenList() : Collections.singleton(node)) {
                Operator<T> op = stat.defaultOperator();
                List<Object> builtArgs = new ArrayList<>();
                if (node.hasChild(KEY_OP) && node.hasChild(KEY_ARGS)) {
                    String opName = node.node(KEY_OP).getString();
                    var args = node.node(KEY_ARGS).childrenList();
                    op = stat.operators().get(opName);
                    if (op == null)
                        throw new SerializationException(node, type, "Invalid operator [" + opName + "] for stat [" + key + "]");
                    if (args.size() != op.args().length)
                        throw new SerializationException(node, type, "Invalid arguments for operator [" + opName + "] for stat [" + key + "]: expected [" +
                                Stream.of(op.args()).map(t -> t.getType().getTypeName()).collect(Collectors.joining(",")) +
                                "], found " + args.size() + " arguments");
                    for (int i = 0; i < args.size(); i++)
                        builtArgs.add(args.get(i).get(op.args()[i]));
                } else {
                    if (op.args().length == 1)
                        builtArgs.add(node.get(op.args()[0]));
                }
                Stat.Node<T> statNode = new Stat.Node<>(stat, op, builtArgs);
                result.chain(key, statNode);
            }
        }

        @Override
        public StatMap deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (types == null)
                throw new SerializationException(node, type, "No types provided");

            Map<Object, ? extends ConfigurationNode> nodes = new HashMap<>(node.childrenMap());
            StatMap result = new StatMap(
                    node.node("priority").get(Priority.class, Priority.DEFAULT),
                    node.node("rule").get(Rule.class, Rule.Constant.TRUE)
            );
            nodes.remove("priority");
            nodes.remove("rule");

            for (var entry : nodes.entrySet()) {
                String key = entry.getKey().toString();
                ConfigurationNode child = entry.getValue();
                Stat<?> stat = types.get(key);
                if (stat == null)
                    throw new SerializationException(child, type, "Invalid stat [" + key + "]");
                add(type, key, child, stat, result);
            }
            return result;
        }
    }

    private final Priority priority;
    private final Rule rule;

    public StatMap(Priority priority, Rule rule) {
        this.priority = priority;
        this.rule = rule;
    }

    public StatMap(StatMap o) {
        super(o);
        priority = o.priority;
        rule = o.rule;
    }

    public Priority priority() { return priority; }
    public Rule rule() { return rule; }

    /**
     * Evaluates the stat node provided by the key passed.
     * @param key The key.
     * @param <T> The type of value.
     * @return The result.
     */
    public <T> Optional<T> val(String key) {
        @SuppressWarnings("unchecked")
        var inst = (Stat.Node<T>) get(key);
        return inst == null ? Optional.empty() : inst.value();
    }

    /**
     * Evaluates the stat node provided by the key passed.
     * @param key The key.
     * @param <T> The type of value.
     * @return The result.
     */
    public <T> Optional<T> val(Stat<T> key) {
        return val(key.key());
    }

    /**
     * Evaluates the stat node provided by the key passed.
     * <p>
     * If there is no value, an exception will be thrown.
     * @param key The key.
     * @param <T> The type of value.
     * @return The result.
     */
    public <T> T req(String key) {
        return this.<T>val(key).orElseThrow(() -> new IllegalStateException("No value for stat [" + key + "]"));
    }

    /**
     * Evaluates the stat node provided by the key passed.
     * <p>
     * If there is no value, an exception will be thrown.
     * @param key The key.
     * @param <T> The type of value.
     * @return The result.
     */
    public <T> T req(Stat<T> key) {
        return req(key.key());
    }

    private <T> void chain(String key, Stat.Node<T> node) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> existing = (Stat.Node<T>) get(key);
        if (existing == null)
            put(node.stat().key(), node.copy());
        else
            existing.chain(node);
    }

    /**
     * Chains a new stat node to an existing stat node, or puts it in this map if it does not exist.
     * <p>
     * If an entry exists in this map, the new node will be chained to the existing node by {@link Stat.Node#chain(Stat.Node)}.
     * <p>
     * If no entry exists in the map, the instance passed will be put.
     * @param node The instance to combine with.
     * @param <T> The type of value.
     */
    public <T> void chain(Stat.Node<T> node) {
        chain(node.stat().key(), node);
    }

    /**
     * Chains all stat nodes of another map to this map.
     * @param o The other stat map.
     * @see #chain(Stat.Node)
     */
    public void chainAll(Map<? extends String, ? extends Stat.Node<?>> o) {
        for (var entry : o.entrySet()) {
            chain(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Converts this map into a map of stat types.
     * @return The result.
     */
    public StatTypes toTypes() {
        StatTypes base = new StatTypes();
        base.defineAll(values());
        return base;
    }

    @Override
    public String toString() {
        return priority + "@" + super.toString();
    }
}
