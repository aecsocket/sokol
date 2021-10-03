package com.gitlab.aecsocket.sokol.core.stat.collection;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores lists of {@link StatMap}s, in forward and reverse order, for use in {@link TreeNode#build()}.
 */
public final class StatLists {
    /**
     * A serializer for a {@link StatLists}.
     */
    public static final class Serializer implements TypeSerializer<StatLists> {
        @Override
        public void serialize(Type type, @Nullable StatLists obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.setList(StatMap.class, obj.joined());
            }
        }

        @Override
        public StatLists deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new StatLists(
                    Serializers.require(node, new TypeToken<List<StatMap>>() {})
            );
        }
    }

    private final List<StatMap> forward;
    private final List<StatMap> reverse;

    public StatLists(List<StatMap> forward, List<StatMap> reverse) {
        this.forward = forward;
        this.reverse = reverse;
    }

    public StatLists(StatLists o) {
        forward = new ArrayList<>();
        for (var map : o.forward)
            forward.add(new StatMap(map));
        reverse = new ArrayList<>();
        for (var map : o.reverse)
            reverse.add(new StatMap(map));
    }

    public StatLists(List<StatMap> joined) {
        forward = new ArrayList<>();
        reverse = new ArrayList<>();
        for (StatMap map : joined) {
            add(map);
        }
    }

    public StatLists() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public List<StatMap> forward() { return forward; }
    public List<StatMap> reverse() { return reverse; }

    public void add(StatMap map) {
        (map.priority().reverse() ? reverse : forward).add(map);
    }

    public void add(StatLists lists) {
        forward.addAll(lists.forward);
        reverse.addAll(lists.reverse);
    }

    /**
     * Gets {@link #forward()} and {@link #reverse()} joined.
     * @return The joined maps.
     */
    public List<StatMap> joined() {
        List<StatMap> result = new ArrayList<>();
        result.addAll(forward);
        result.addAll(reverse);
        return result;
    }

    @Override
    public String toString() {
        return "%s | %s".formatted(forward, reverse);
    }
}
