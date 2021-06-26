package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.component.Blueprint;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;

public final class PaperBlueprint extends Blueprint<PaperTreeNode> {
    public static final class Serializer implements TypeSerializer<PaperBlueprint> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        public SokolPlugin plugin() { return plugin; }

        @Override
        public void serialize(Type type, @Nullable PaperBlueprint obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(obj.node);
            }
        }

        @Override
        public PaperBlueprint deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new PaperBlueprint(plugin,
                    Objects.toString(node.key()),
                    Serializers.require(node, PaperTreeNode.class)
            );
        }
    }

    public PaperBlueprint(SokolPlatform platform, String id, PaperTreeNode node) {
        super(platform, id, node);
    }
}
