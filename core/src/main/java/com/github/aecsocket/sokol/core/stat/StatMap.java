package com.github.aecsocket.sokol.core.stat;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

import static net.kyori.adventure.text.Component.*;

public final class StatMap extends HashMap<String, Stat.Node<?>> {
    public static final String
        STAT_MAP_HEADER = "stat_map.header",
        STAT_MAP_ENTRY_SINGLE = "stat_map.entry.single",
        STAT_MAP_ENTRY_MULTIPLE = "stat_map.entry.multiple";

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
                ours.chain(node.copy());
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

    private <T> List<Component> render(I18N i18n, Locale locale, String key, Stat.Node<T> node) {
        return i18n.lines(locale, node.next() == null ? STAT_MAP_ENTRY_SINGLE : STAT_MAP_ENTRY_MULTIPLE,
            c -> c.of("key", () -> text(key)),
            c -> c.of("name", () -> c.rd(node.stat())),
            c -> c.of("value", () -> node.stat().renderValue(i18n, locale, node.compute())),
            c -> c.of("length", () -> text(node.size())),
            c -> c.of("stat", () -> c.rd(node)));
    }

    public List<Component> render(I18N i18n, Locale locale) {
        List<Component> lines = new ArrayList<>(i18n.lines(locale, STAT_MAP_HEADER,
            c -> c.of("amount", () -> text(size()))));
        for (var entry : entrySet()) {
            lines.addAll(render(i18n, locale, entry.getKey(), entry.getValue()));
        }
        return lines;
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
                if (!(nodes = new ArrayList<>(opNode.childrenList())).isEmpty() && nodes.get(0).raw() instanceof String opName) {
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
