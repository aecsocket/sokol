package com.github.aecsocket.sokol.core.stat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.i18n.Renderable;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.rule.Rule;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import static net.kyori.adventure.text.Component.*;

public final class StatIntermediate {
    public record Priority(int value, boolean reverse) implements Renderable {
        public static final Priority MIN = forwardPriority(Integer.MIN_VALUE);
        public static final Priority MAX = reversePriority(Integer.MAX_VALUE);
        public static final Priority DEFAULT = forwardPriority(0);

        public static final String
            STAT_PRIORITY_FORWARD = "stat_priority.forward",
            STAT_PRIORITY_REVERSE = "stat_priority.reverse";

        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, reverse ? STAT_PRIORITY_REVERSE : STAT_PRIORITY_FORWARD,
                c -> c.of("value", () ->
                    value == Integer.MAX_VALUE ? text("∞") :
                    value == Integer.MIN_VALUE ? text("-∞") :
                    text(value)));
        }

        @Override
        public String toString() {
            return reverse ? "(" + value + ")" : ""+value;
        }

        public static final class Serializer implements TypeSerializer<Priority> {
            public static final Serializer INSTANCE = new Serializer();

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
                    return reversePriority(Serializers.require(nodes.get(0), int.class));
                }
                return forwardPriority(Serializers.require(node, int.class));
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
    public record MapData(@Required StatMap entries, @Required Priority priority, @Required Rule rule) {}

    private final List<MapData> forward;
    private final List<MapData> reverse;

    public StatIntermediate(List<MapData> forward, List<MapData> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public StatIntermediate(StatIntermediate o) {
        forward = new ArrayList<>();
        for (var map : o.forward)
            forward.add(new MapData(new StatMap(map.entries), map.priority, map.rule));
        reverse = new ArrayList<>();
        for (var map : o.reverse)
            reverse.add(new MapData(new StatMap(map.entries), map.priority, map.rule));
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

    public void onBoth(BiConsumer<List<MapData>, Boolean> function) {
        function.accept(forward, false);
        function.accept(reverse, true);
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
                result.add(Serializers.require(child, MapData.class));
            }
            return result;
        }
    }
}
