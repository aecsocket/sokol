package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Renderable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Stat<T> extends Renderable {
    String key();
    Optional<T> defaultValue();

    Value<T> deserialize(Type type, ConfigurationNode node) throws SerializationException;

    Node<T> node(Value<T> value);

    @Override
    default net.kyori.adventure.text.Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, "stat." + key() + ".name");
    }

    interface Value<T> extends Renderable {
        T compute(T cur);
    }

    interface InitialValue<T> extends Value<T> {
        T first();
    }

    final class Node<T> {
        private final Stat<T> stat;
        private final Value<T> value;
        private @Nullable Node<T> next;

        public Node(Stat<T> stat, Value<T> value) {
            if (!(value instanceof InitialValue))
                throw new IllegalStateException("Attempting to start stat node chain with non-initial value");
            this.stat = stat;
            this.value = value;
        }

        public Stat<T> stat() { return stat; }
        public Value<T> value() { return value; }

        public T compute() {
            if (!(value instanceof InitialValue<T> initial))
                throw new IllegalStateException("Start of stat node chain is a non-initial value");
            Node<T> node = next;
            T value = initial.first();
            for (; node != null; node = node.next)
                value = node.value.compute(value);
            return value;
        }

        public @Nullable Node<T> next() { return next; }
        public Node<T> chain(Node<T> next) {
            if (this.next == null)
                this.next = next;
            else
                this.next.chain(next);
            return this;
        }

        public List<Node<T>> asList() {
            List<Node<T>> result = new LinkedList<>();
            Node<T> node = this;
            for (; node != null; node = node.next)
                result.add(node);
            return result;
        }
    }

    final class OperationDeserializer<T> {
        @FunctionalInterface
        interface Operation<T> {
            Value<T> createValue(Type type, ConfigurationNode node, ConfigurationNode... args) throws SerializationException;
        }

        record OperationData<T>(
                Operation<T> operation,
                String[] fields
        ) {}

        private final Map<String, OperationData<T>> operations;
        private final @Nullable String defaultOperation;

        private OperationDeserializer(Map<String, OperationData<T>> operations, @Nullable String defaultOperation) {
            this.operations = Collections.unmodifiableMap(operations);
            this.defaultOperation = defaultOperation;
        }

        private String formatArgs(Class<?>... types) {
            return Stream.of(types).map(Class::getSimpleName).collect(Collectors.joining(", "));
        }

        public Value<T> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            List<? extends ConfigurationNode> nodes = node.isList() ? new ArrayList<>(node.childrenList()) : null;
            OperationData<T> op;
            if (nodes != null && nodes.size() > 0 && nodes.get(0).raw() instanceof String opName) {
                op = operations.get(opName);
                if (op == null)
                    throw new SerializationException(node, type, "Invalid operation type '" + opName + "'");
                nodes.remove(0);

                if (op.fields.length != nodes.size())
                    throw new SerializationException(node, type, "Operation '" + opName + "' requires fields: [" +
                            String.join(", ", op.fields) + "], passed " + nodes.size());
                return op.operation.createValue(type, node, nodes.toArray(new ConfigurationNode[0]));
            } else {
                op = operations.get(defaultOperation);
                if (op == null)
                    throw new SerializationException(node, type, "No operation provided");
                if (op.fields.length != 1)
                    throw new SerializationException(node, type, "Operation '" + defaultOperation + "' requires fields: [" +
                            String.join(", ", op.fields) + "]");
                return op.operation.createValue(type, node, node);
            }
        }

        public static final class Builder<T> {
            private final Map<String, OperationData<T>> operations = new HashMap<>();
            private @Nullable String defaultOperation;

            public Builder<T> operation(String key, Operation<T> operation, String... fields) {
                operations.put(key, new OperationData<>(operation, fields));
                return this;
            }

            public Builder<T> defaultOperation(String defaultOperation) {
                this.defaultOperation = defaultOperation;
                return this;
            }

            public OperationDeserializer<T> build() {
                return new OperationDeserializer<>(operations, defaultOperation);
            }
        }

        static <T> Builder<T> builder() {
            return new Builder<>();
        }
    }
}
