package com.github.aecsocket.sokol.core.impl;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.api.Blueprint;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.minecommons.core.i18n.I18N;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class KeyedBlueprint<B extends Blueprint.Scoped<B, ?, ?, ?>> implements Keyed {
    public static final String I18N_KEY = "blueprint";

    private final SokolPlatform platform;
    private final String id;
    private final B blueprint;

    public KeyedBlueprint(SokolPlatform platform, String id, B blueprint) {
        this.platform = platform;
        this.id = id;
        this.blueprint = blueprint;
    }

    @Override public String id() { return id; }

    public B blueprint() {
        return blueprint.copy();
    }

    @Override
    public net.kyori.adventure.text.Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, I18N_KEY + "." + id + "." + NAME);
    }

    @Override
    public Optional<List<net.kyori.adventure.text.Component>> renderDescription(I18N i18n, Locale locale) {
        return i18n.orLines(locale, I18N_KEY + "." + id + "." + DESCRIPTION);
    }

    public static final class Serializer<B extends Blueprint.Scoped<B, ?, ?, ?>> implements TypeSerializer<KeyedBlueprint<B>> {
        private final SokolPlatform platform;
        private final Class<B> clazz;

        public Serializer(SokolPlatform platform, Class<B> clazz) {
            this.platform = platform;
            this.clazz = clazz;
        }

        public SokolPlatform platform() { return platform; }
        public Class<B> clazz() { return clazz; }

        @Override
        public void serialize(Type type, @Nullable KeyedBlueprint<B> obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(obj.blueprint);
            }
        }

        @Override
        public KeyedBlueprint<B> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new KeyedBlueprint<>(platform, ""+Objects.requireNonNull(node.key()), Serializers.require(node, clazz));
        }
    }
}
