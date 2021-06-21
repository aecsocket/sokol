package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Point2;
import com.gitlab.aecsocket.sokol.core.component.AbstractSlot;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

@ConfigSerializable
public final class PaperSlot extends AbstractSlot<PaperComponent> {
    public static final String TAG_INTERNAL = "internal";

    public static final class Serializer implements TypeSerializer<PaperSlot> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        @Override
        public void serialize(Type type, @Nullable PaperSlot obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.node("tags").set(obj.tags);
                node.node("offset").set(obj.offset);
            }
        }

        @Override
        public PaperSlot deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new PaperSlot(plugin,
                    node.node("tags").getList(String.class, Collections.emptyList()),
                    node.node("rule").get(Rule.class, Rule.Constant.TRUE),
                    node.node("offset").get(Point2.class, Point2.ZERO)
            );
        }
    }

    private final SokolPlugin platform;
    private final Point2 offset;

    public PaperSlot(SokolPlugin platform, Collection<String> tags, Rule rule, String key, PaperComponent parent, Point2 offset) {
        super(tags, rule, key, parent);
        this.platform = platform;
        this.offset = offset;
    }

    public PaperSlot(SokolPlugin platform, Collection<String> tags, Rule rule, Point2 offset) {
        super(tags, rule);
        this.platform = platform;
        this.offset = offset;
    }

    private PaperSlot(SokolPlugin platform) {
        this(platform, Collections.emptySet(), Rule.Constant.TRUE, Point2.ZERO);
    }

    @Override public @NotNull SokolPlugin platform() { return platform; }
    public @NotNull Point2 offset() { return offset; }

    @Override protected @NotNull Class<PaperComponent> componentType() { return PaperComponent.class; }

    public boolean internal() { return tagged(TAG_INTERNAL); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaperSlot paperSlot = (PaperSlot) o;
        return offset.equals(paperSlot.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), offset);
    }

    @Override
    public String toString() {
        return "PaperSlot:" + key + "{" +
                "tags=" + tags +
                ", rule=" + rule +
                ", offset=" + offset +
                '}';
    }
}
