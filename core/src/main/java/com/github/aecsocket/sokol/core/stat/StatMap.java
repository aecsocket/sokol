package com.github.aecsocket.sokol.core.stat;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public final class StatMap extends HashMap<String, Stat.Node<?>> {
    public StatMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public StatMap(int initialCapacity) {
        super(initialCapacity);
    }

    public StatMap() {}

    public StatMap(Map<? extends String, ? extends Stat.Node<?>> m) {
        super(m);
    }

    public <T> Optional<T> value(String key) throws StatAccessException {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key);
        return node == null ? Optional.empty() : Optional.of(node.compute());
    }

    public <T> Optional<T> value(Stat<T> key) throws StatAccessException {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key.key);
        return node == null ? key.defaultValue() : Optional.of(node.compute());
    }

    public <T> T require(Stat<T> key) throws StatAccessException {
        return value(key)
            .orElseThrow(() -> new StatAccessException("No value for stat `" + key.key + "`"));
    }

    private <T> void chain(String key, Stat.Node<T> node) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> ours = (Stat.Node<T>) get(key);
        Stat.Node<T> copy = node.copy();
        if (ours == null) {
            put(key, copy);
        } else {
            if (node.op() instanceof Stat.Op.Discards)
                put(key, copy);
            else
                ours.next(node.copy());
        }
    }

    public <T> void chain(Stat.Node<T> op) {
        chain(op.stat().key, op);
    }

    public <T> void chain(StatMap o) {
        for (var entry : o.entrySet()) {
            chain(entry.getKey(), entry.getValue());
        }
    }

    public static final class Serializer implements TypeSerializer<StatMap> {
        private @Nullable Map<String, Stat<?>> types;

        public Map<String, Stat<?>> types() { return types; }
        public void types(@Nullable Map<String, Stat<?>> types) { this.types = types; }

        @Override
        public void serialize(Type type, @Nullable StatMap obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public StatMap deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (types == null)
                throw new SerializationException(node, type, "No types provided");

            if (!node.isMap())
                throw new SerializationException(node, type, "Stats must be expressed as a map");

            StatMap result = new StatMap();
            for (var entry : node.childrenMap().entrySet()) {
                String key = entry.getKey()+"";
                ConfigurationNode value = entry.getValue();
                Stat<?> stat = types.get(key);
                if (stat == null)
                    throw new SerializationException(node, type, "Invalid stat `" + key + "`, accepts: [" + String.join(", ", types.keySet()) + "]");
                add(type, key, value, stat, result);
            }
            return result;
        }

        private <T> void add(Type type, String key, ConfigurationNode node, Stat<T> stat, StatMap result) throws SerializationException {
            if (!node.isList())
                throw new SerializationException(node, type, "Stat must be expressed as a list of all operations");
            var opTypes = stat.opTypes();
            for (var opNode : node.childrenList()) {
                Stat.OpType<T> opType;
                ConfigurationNode[] args;
                List<? extends ConfigurationNode> nodes;
                if (!(nodes = opNode.childrenList()).isEmpty() && nodes.get(0).raw() instanceof String opName) {
                    opType = opTypes.get(opName);
                    if (opType == null)
                        throw new SerializationException(opNode, type, "Invalid operation type `" + opName + "`");
                    nodes.remove(0);
                    args = nodes.toArray(new ConfigurationNode[0]);
                } else {
                    opType = opTypes.defaultOp()
                        .orElseThrow(() -> new SerializationException(opNode, type, "No default operation - you must provide an operator"));
                    args = new ConfigurationNode[] { opNode };
                }

                if (opType.args().length != args.length)
                    throw new SerializationException(opNode, type, "Operation `" + opType.name() + "` requires [" +
                            String.join(", ", opType.args()) + "], found " + nodes.size());
                Stat.Op<T> op = opType.create(type, opNode, args);
                result.chain(stat.node(op));
            }
        }
    }
}
