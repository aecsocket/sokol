package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.minecommons.core.Validation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class StatMap extends HashMap<String, Stat.Instance<?>> {
    public record Priority(int value, boolean reverse) {
        public static final Priority DEFAULT = new Priority(0, false);

        public Priority {
            Validation.greaterThanEquals("value", value, 0);
        }

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

    public static final class Serializer implements TypeSerializer<StatMap> {
        private Map<String, Stat<?>> base;

        public Map<String, Stat<?>> base() { return base; }
        public void base(Map<String, Stat<?>> base) { this.base = base; }

        @Override
        public void serialize(Type type, @Nullable StatMap obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(obj.toBase());
            }
        }

        private <T> void add(String key, ConfigurationNode node, Stat<T> stat, StatMap result) throws SerializationException {
            result.put(key, stat.instance(node.get(stat.type())));
        }

        @Override
        public StatMap deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (base == null)
                throw new SerializationException(node, type, "No base map provided");

            Map<Object, ? extends ConfigurationNode> nodes = new HashMap<>(node.childrenMap());
            StatMap result = new StatMap(
                    node.node("priority").get(Priority.class, Priority.DEFAULT)
            );
            nodes.remove("priority");

            for (var entry : nodes.entrySet()) {
                String key = entry.getKey().toString();
                Stat<?> stat = base.get(key);
                if (stat == null)
                    continue;
                add(key, entry.getValue(), stat, result);
            }
            return result;
        }
    }

    private final Priority priority;

    public StatMap(Priority priority) {
        this.priority = priority;
    }

    public Priority priority() { return priority; }

    public <T> T value(String key) {
        @SuppressWarnings("unchecked")
        Stat.Instance<T> inst = (Stat.Instance<T>) get(key);
        return inst == null ? null : inst.value();
    }

    public <T> T value(String key, T value) {
        @SuppressWarnings("unchecked")
        Stat.Instance<T> inst = (Stat.Instance<T>) get(key);
        return inst == null ? null : inst.value(value);
    }

    public <T> void combine(String key, Stat.Instance<T> instance) {
        @SuppressWarnings("unchecked")
        Stat.Instance<T> existing = (Stat.Instance<T>) get(key);
        if (existing == null)
            put(key, instance);
        else
            existing.combineFrom(instance);
    }

    public void combineAll(Map<? extends String, ? extends Stat.Instance<?>> o) {
        for (var entry : o.entrySet()) {
            combine(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Stat<?>> toBase() {
        Map<String, Stat<?>> base = new HashMap<>();
        for (var entry : entrySet()) {
            base.put(entry.getKey(), entry.getValue().stat());
        }
        return base;
    }

    public void fromBase(Map<String, Stat<?>> base) {
        for (var entry : base.entrySet()) {
            put(entry.getKey(), entry.getValue().instance(null));
        }
    }

    @Override
    public String toString() {
        return "(%s) %s".formatted(priority, super.toString());
    }
}
