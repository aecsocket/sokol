package com.gitlab.aecsocket.sokol.core.node;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;

public interface NodePath {
    static NodePath empty() {
        return EmptyNodePath.INSTANCE;
    }

    static NodePath path(List<String> path) {
        return new ListNodePath(path);
    }

    static NodePath path(String... path) {
        return new ArrayNodePath(path);
    }

    int size();
    String get(int idx);
    @Nullable String last();

    List<String> list();
    String[] array();

    default NodePath append(String... nodes) {
        List<String> allNodes = new ArrayList<>(list());
        allNodes.addAll(Arrays.asList(nodes));
        return path(allNodes);
    }

    final class Serializer implements TypeSerializer<NodePath> {
        @Override
        public void serialize(Type type, @Nullable NodePath obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.setList(String.class, obj.list());
            }
        }

        @Override
        public NodePath deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (!node.isList())
                throw new SerializationException(node, type, "Node path must be list");
            List<String> path = new ArrayList<>();
            for (var elem : node.childrenList()) {
                path.add(require(elem, String.class));
            }
            return NodePath.path(path);
        }
    }
}
