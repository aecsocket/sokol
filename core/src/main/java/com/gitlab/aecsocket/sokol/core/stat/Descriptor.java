package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Descriptor<T>(Operator<T> operator, List<Object> args) {
    public static abstract class Serializer<T> implements TypeSerializer<Descriptor<T>> {
        protected abstract Operators<T> operators();

        @Override
        public void serialize(Type type, @Nullable Descriptor<T> obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.appendListNode().set(obj.operator.key());
                for (var arg : obj.args)
                    node.appendListNode().set(arg);
            }
        }

        private String formatArgs(TypeToken<?>[] args) {
            return "[" + Stream.of(args).map(t -> t.getType().getTypeName()).collect(Collectors.joining(", ")) + "]";
        }

        @Override
        public Descriptor<T> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            var op = operators().defOp();
            List<? extends ConfigurationNode> nodes;
            if (node.isList() && node.node(0).rawScalar() instanceof String opKey) {
                op = operators().operators().get(opKey);
                if (op == null)
                    throw new SerializationException(node, type, "Invalid operation [" + opKey + "]");
                nodes = new ArrayList<>(node.childrenList());
                nodes.remove(0);
            } else
                nodes = null;
            if (op == null)
                throw new SerializationException(node, type, "No operation passed");

            List<Object> args = new ArrayList<>();
            if (nodes == null) {
                if (op.args().length > 1)
                    throw new SerializationException(node, type, "Passing one value as argument, expected " +
                            formatArgs(op.args()));
                if (op.args().length == 1)
                    args.add(Serializers.require(node, op.args()[0]));
            } else {
                int nArgs = nodes.size();
                if (nArgs != op.args().length)
                    throw new SerializationException(node, type, "Invalid number of arguments: found " + nArgs + ", expected " +
                            formatArgs(op.args()));
                for (int i = 0; i < op.args().length; i++) {
                    args.add(Serializers.require(nodes.get(i), op.args()[i]));
                }
            }

            return new Descriptor<>(op, args);
        }
    }

    public static <T> Descriptor<T> single(Operator<T> op, T val) {
        return new Descriptor<>(op, Collections.singletonList(val));
    }

    public T combine(@Nullable T b) {
        return operator.operate(b, args);
    }

    public Descriptor<T> copy() {
        return new Descriptor<>(operator, new ArrayList<>(args));
    }

    @Override
    public String toString() {
        return operator.key() + " " + args;
    }
}
