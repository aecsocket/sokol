package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class StatLists {
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
                    node.require(new TypeToken<List<StatMap>>() {})
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
        forward = new ArrayList<>(o.forward);
        reverse = new ArrayList<>(o.reverse);
    }

    public StatLists(List<StatMap> joined) {
        forward = new ArrayList<>();
        reverse = new ArrayList<>();
        for (StatMap map : joined) {
            (map.priority().reverse() ? reverse : forward).add(map);
        }
    }

    public StatLists() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public List<StatMap> forward() { return forward; }
    public List<StatMap> reverse() { return reverse; }

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
