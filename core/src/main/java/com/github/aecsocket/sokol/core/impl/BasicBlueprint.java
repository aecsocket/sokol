package com.github.aecsocket.sokol.core.impl;

import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.Blueprint;
import com.github.aecsocket.sokol.core.BlueprintNode;
import com.github.aecsocket.sokol.core.SokolPlatform;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class BasicBlueprint<
    N extends BlueprintNode.Scoped<N, ?, ?, ?>
> implements Blueprint<N> {
    protected final String id;
    protected final N blueprint;

    public BasicBlueprint(String id, N blueprint) {
        this.id = id;
        this.blueprint = blueprint;
    }

    @Override public String id() { return id; }

    @Override
    public N create() {
        return blueprint.copy();
    }

    public static abstract class Serializer<
        B extends Blueprint<N>,
        N extends BlueprintNode.Scoped<N, ?, ?, ?>
    > implements TypeSerializer<B> {
        protected abstract Class<N> nodeType();

        protected abstract B create(
            String id, N blueprint
        );

        @Override
        public void serialize(Type type, @Nullable B obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public B deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return create(
                SokolPlatform.idByKey(type, node),
                Serializers.require(node, nodeType())
            );
        }
    }
}
