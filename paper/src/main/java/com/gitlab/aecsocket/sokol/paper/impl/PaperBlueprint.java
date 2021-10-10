package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.impl.AbstractBlueprint;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public final class PaperBlueprint extends AbstractBlueprint<PaperNode> {
    private final SokolPlugin platform;

    public PaperBlueprint(SokolPlugin platform, String id, PaperNode node) {
        super(id, node);
        this.platform = platform;
    }

    @Override public SokolPlugin platform() { return platform; }

    public static final class Serializer implements TypeSerializer<PaperBlueprint> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void serialize(Type type, @Nullable PaperBlueprint obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public PaperBlueprint deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Utils.id(type, node);
            return new PaperBlueprint(plugin, id, require(node, PaperNode.class));
        }
    }
}
