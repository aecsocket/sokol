package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public final class StatIntermediate {
    public record Priority(int value, boolean reverse) {
        public static final Priority MIN = forwardPriority(Integer.MIN_VALUE);
        public static final Priority MAX = reversePriority(Integer.MAX_VALUE);
        public static final Priority DEFAULT = forwardPriority(0);

        @Override
        public String toString() {
            return reverse ? "(" + value + ")" : ""+value;
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
                if (node.isList()) {
                    List<? extends ConfigurationNode> nodes = node.childrenList();
                    if (nodes.size() != 1)
                        throw new SerializationException("Reverse priority must have 1 number element");
                    return reversePriority(require(nodes.get(0), int.class));
                }
                return forwardPriority(require(node, int.class));
            }
        }
    }

    public static Priority forwardPriority(int value) {
        return new Priority(value, false);
    }

    public static Priority reversePriority(int value) {
        return new Priority(value, true);
    }

    @ConfigSerializable
    public record MapData(StatMap stats, Priority priority, Rule rule) {}

    private final List<MapData> forward;
    private final List<MapData> reverse;

    public StatIntermediate(List<MapData> forward, List<MapData> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public StatIntermediate(StatIntermediate o) {
        forward = new ArrayList<>();
        for (var map : o.forward)
            forward.add(new MapData(new StatMap(map.stats), map.priority, map.rule));
        reverse = new ArrayList<>();
        for (var map : o.reverse)
            reverse.add(new MapData(new StatMap(map.stats), map.priority, map.rule));
    }

    public StatIntermediate() {
        forward = new ArrayList<>();
        reverse = new ArrayList<>();
    }

    public List<MapData> forward() { return forward; }
    public List<MapData> reverse() { return reverse; }

    public List<MapData> join() {
        List<MapData> result = new ArrayList<>();
        result.addAll(reverse);
        result.addAll(forward);
        return result;
    }

    public void add(MapData data) {
        if (data.priority.reverse)
            reverse.add(data);
        else
            forward.add(data);
    }

    public void addForward(StatMap stats, Priority priority, Rule rule) {
        forward.add(new MapData(stats, priority, rule));
    }

    public void addReverse(StatMap stats, Priority priority, Rule rule) {
        reverse.add(new MapData(stats, priority, rule));
    }

    public static final class Serializer implements TypeSerializer<StatIntermediate> {
        @Override
        public void serialize(Type type, @Nullable StatIntermediate obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.setList(MapData.class, obj.join());
            }
        }

        @Override
        public StatIntermediate deserialize(Type type, ConfigurationNode node) throws SerializationException {
            if (!node.isList())
                throw new SerializationException(node, type, "Stat intermediate must be a list");

            StatIntermediate result = new StatIntermediate();
            for (var child : node.childrenList()) {
                result.add(require(child, MapData.class));
            }
            return result;
        }
    }
}
