package com.gitlab.aecsocket.sokol.core.system.util;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

public record SystemPath(String system, String[] nodes) {
    public static final class Serializer implements TypeSerializer<SystemPath> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void serialize(Type type, @Nullable SystemPath obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.appendListNode().set(obj.nodes);
                node.appendListNode().set(obj.system);
            }
        }

        @Override
        public SystemPath deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new SystemPath(
                    Serializers.require(node.node(1), String.class),
                    Serializers.require(node.node(0), String[].class)
            );
        }
    }

    public static SystemPath path(String system, String... nodes) {
        return new SystemPath(system, nodes);
    }

    public static SystemPath path(System.Instance system) {
        return path(system.base().id(), system.parent().path());
    }

    @SuppressWarnings("unchecked")
    public <Y extends System.Instance> Optional<Y> get(TreeNode node) {
        return node.node(nodes).flatMap(n -> (Optional<Y>) n.system(system));
    }

    @Override public String toString() { return Arrays.toString(nodes) + ":" + system; }
}
