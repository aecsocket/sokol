package com.github.aecsocket.sokol.core.stat;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class StatMap extends HashMap<String, Stat.Node<?>> {
    public StatMap(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public StatMap(int initialCapacity) { super(initialCapacity); }
    public StatMap() {}
    public StatMap(Map<? extends String, ? extends Stat.Node<?>> m) { super(m); }

    public <T> Optional<Stat.Node<T>> node(String key) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key);
        return Optional.ofNullable(node);
    }

    public <T> Optional<Stat.Node<T>> node(Stat<T> key) {
        return node(key.key());
    }

    public <T> Optional<T> value(String key) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key);
        return node == null ? Optional.empty() : Optional.ofNullable(node.compute());
    }

    public <T> Optional<T> value(Stat<T> key) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key.key());
        return node == null ? key.defaultValue() : Optional.ofNullable(node.compute());
    }

    public <T> T require(Stat<T> key) throws IllegalStateException {
        return value(key)
                .orElseThrow(() -> new IllegalStateException("No value for stat '" + key.key() + "'"));
    }

    public <T> StatMap set(Stat<T> key, Stat.Value<T> val) {
        put(key.key(), key.node(val));
        return this;
    }

    private <T> void chain(String key, Stat.Node<T> node) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> ours = (Stat.Node<T>) get(key);
        var copy = new Stat.Node<>(node);
        if (ours == null)
            put(key, copy);
        else {
            if (node.value().discardsPrevious())
                put(key, copy);
            else
                ours.chain(copy);
        }
    }

    public <T> void chain(Stat.Node<T> node) {
        chain(node.stat().key(), node);
    }

    public void chain(StatMap o) {
        for (var entry : o.entrySet()) {
            chain(entry.getKey(), entry.getValue());
        }
    }

    public static final class Serializer implements TypeSerializer<StatMap> {
        private @Nullable Map<String, Stat<?>> types;

        public @Nullable Map<String, Stat<?>> types() { return types; }
        public void types(@Nullable Map<String, Stat<?>> types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable StatMap obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                throw new UnsupportedOperationException();
            }
        }

        private <T> void add(Type type, String key, ConfigurationNode node, Stat<T> stat, StatMap result) throws SerializationException {
            if (!node.isList())
                throw new SerializationException(node, type, "Stat must be a chain of all values, represented by an array");
            for (var child : node.childrenList()) {
                Stat.Value<T> value = stat.deserialize(type, child);
                result.chain(key, stat.node(value));
            }
        }

        @Override
        public StatMap deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (types == null)
                throw new SerializationException(node, type, "No stats provided");

            if (!node.isMap())
                throw new SerializationException(node, type, "Stats must be a map");

            StatMap result = new StatMap();
            for (var entry : node.childrenMap().entrySet()) {
                String key = entry.getKey()+"";
                ConfigurationNode value = entry.getValue();
                Stat<?> stat = types.get(key);
                if (stat == null)
                    throw new SerializationException(value, type, "Invalid stat '" + key + "'");
                add(type, key, value, stat, result);
            }
            return result;
        }
    }
}
