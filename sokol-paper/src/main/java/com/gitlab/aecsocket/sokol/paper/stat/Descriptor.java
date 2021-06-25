package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.core.Validation;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.Copier;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Map;

public record Descriptor<T>(
        String operator,
        T value
) {
    public static final class Serializer<T> implements TypeSerializer<Descriptor<T>> {
        private final TypeToken<T> type;

        public Serializer(TypeToken<T> type) {
            this.type = type;
        }

        public TypeToken<T> type() { return type; }

        @Override
        public void serialize(Type type, @Nullable Descriptor<T> obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                if (obj.operator() == null)
                    node.set(obj.value());
                else {
                    node.appendListNode().set(obj.operator());
                    node.appendListNode().set(obj.value());
                }
            }
        }

        @Override
        public Descriptor<T> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (node.isList() && node.childrenList().size() > 0 && node.node(0).rawScalar() instanceof String) {
                T value = Serializers.require(node.node(1), this.type);
                return new Descriptor<>(
                        Serializers.require(node.node(0), String.class),
                        value
                );
            }
            T value = Serializers.require(node, this.type);
            return new Descriptor<>(null, value);
        }
    }

    public Descriptor {
        Validation.notNull(value, "value");
    }

    public Descriptor(T value) {
        this(null, value);
    }

    public Descriptor<T> operate(Map<String, Combiner<T>> operations, String defaultOperator, Descriptor<T> v) {
        Combiner<T> combiner = operations.getOrDefault(operator, operations.get(defaultOperator));
        if (combiner == null)
            throw new IllegalStateException("Invalid operator [" + operator + "], valid: " + operations.keySet());
        return new Descriptor<>(null, combiner.combine(value, v.value));
    }

    public Descriptor<T> copy(Copier<T> copier) {
        return new Descriptor<>(operator, copier.copy(value));
    }

    @Override
    public String toString() {
        return (operator == null ? "" : operator) + value;
    }
}
