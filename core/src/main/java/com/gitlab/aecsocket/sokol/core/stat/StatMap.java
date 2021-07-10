package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.minecommons.core.Validation;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A map of string names to stat instances.
 */
public class StatMap extends HashMap<String, Stat.Instance<?>> {
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
     * For deserialization, requires the base stat types to be specified using {@link #types(Map)}.
     */
    public static final class Serializer implements TypeSerializer<StatMap> {
        private @Nullable Map<String, Stat<?>> types;

        public @Nullable Map<String, Stat<?>> types() { return types; }
        public void types(@Nullable Map<String, Stat<?>> types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable StatMap obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(obj.toTypes());
                node.node("priority").set(obj.priority);
            }
        }

        private <T> void add(String key, ConfigurationNode node, Stat<T> stat, StatMap result) throws SerializationException {
            result.put(key, stat.instance(Serializers.require(node, stat.type())));
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
                Stat<?> stat = types.get(key);
                if (stat == null)
                    continue;
                add(key, entry.getValue(), stat, result);
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

    public Priority priority() { return priority; }
    public Rule rule() { return rule; }

    /**
     * Sets a value of a stat instance by its stat key.
     * <p>
     * If the stat instance is not present, no action will be performed.
     * @param key The key.
     * @param value The value to set the instance to.
     * @param <T> The type of value.
     */
    public <T> void set(String key, @Nullable T value) {
        @SuppressWarnings("unchecked")
        Stat.Instance<T> inst = (Stat.Instance<T>) get(key);
        if (inst != null)
            inst.value(value);
    }

    /**
     * Gets a value of a stat instance by its stat key.
     * @param key The key.
     * @param <T> The type of value.
     * @return An Optional of the result.
     */
    public <T> Optional<T> val(String key) {
        @SuppressWarnings("unchecked")
        Stat.Instance<T> inst = (Stat.Instance<T>) get(key);
        return inst == null ? Optional.empty() : Optional.ofNullable(inst.value());
    }

    /**
     * Gets a value of a stat instance's stat descriptor by its stat key.
     * @param key The key.
     * @param <T> The type of value.
     * @return An Optional of the result.
     */
    public <T> Optional<T> descVal(String key) {
        @SuppressWarnings("unchecked")
        Stat.Instance<StatDescriptor<T>> inst = (Stat.Instance<StatDescriptor<T>>) get(key);
        return inst == null ? Optional.empty() : Optional.ofNullable(inst.value().value());
    }

    /**
     * Gets a value of a stat instance by its stat key.
     * <p>
     * If there is no value, an exception is thrown.
     * @param key The key.
     * @param <T> The type of value.
     * @return The result.
     */
    public <T> T require(String key) {
        return this.<T>val(key).orElseThrow(() -> new IllegalStateException("No value for key [" + key + "]"));
    }

    /**
     * Gets a value of a stat instance's stat descriptor by its stat key.
     * <p>
     * If there is no value, an exception is thrown.
     * @param key The key.
     * @param <T> The type of value.
     * @return An Optional of the result.
     */
    public <T> T descRequire(String key) {
        return this.<T>descVal(key).orElseThrow(() -> new IllegalStateException("No value for key [" + key + "]"));
    }

    /**
     * Combines another stat instance with an instance in this map.
     * <p>
     * If an entry exists in this map, the existing entry's value will be set using {@link Stat.Instance#combineFrom(Stat.Instance)}.
     * <p>
     * If no entry exists in the map, the instance passed will be put.
     * @param key The key of the stat.
     * @param instance The instance to combine with.
     * @param <T> The type of value.
     */
    public <T> void combine(String key, Stat.Instance<T> instance) {
        @SuppressWarnings("unchecked")
        Stat.Instance<T> existing = (Stat.Instance<T>) get(key);
        if (existing == null)
            put(key, instance.copy());
        else
            existing.combineFrom(instance);
    }

    /**
     * Combines all entries of another stat map with this map.
     * @param o The other stat map.
     * @see #combine(String, Stat.Instance)
     */
    public void combineAll(Map<? extends String, ? extends Stat.Instance<?>> o) {
        for (var entry : o.entrySet()) {
            combine(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Converts this map into a map of stat types.
     * @return The result.
     */
    public Map<String, Stat<?>> toTypes() {
        Map<String, Stat<?>> base = new HashMap<>();
        for (var entry : entrySet()) {
            base.put(entry.getKey(), entry.getValue().stat());
        }
        return base;
    }

    @Override
    public String toString() {
        return priority + super.toString();
    }
}
