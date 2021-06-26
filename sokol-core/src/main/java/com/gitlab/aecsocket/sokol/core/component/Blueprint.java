package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.tree.ImmutableNode;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Objects;

public class Blueprint<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> implements Keyed {
    public static final class Serializer<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> implements TypeSerializer<Blueprint<N>> {
        private final SokolPlatform platform;
        private final Class<N> nodeType;

        public Serializer(SokolPlatform platform, Class<N> nodeType) {
            this.platform = platform;
            this.nodeType = nodeType;
        }

        public SokolPlatform platform() { return platform; }
        public Class<N> nodeType() { return nodeType; }

        @Override
        public void serialize(Type type, @Nullable Blueprint<N> obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                node.set(obj.node);
            }
        }

        @Override
        public Blueprint<N> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new Blueprint<>(platform,
                    Objects.toString(node.key()),
                    ImmutableNode.of(Serializers.require(node, nodeType))
            );
        }
    }

    protected final SokolPlatform platform;
    protected final String id;
    protected final N node;

    public Blueprint(SokolPlatform platform, String id, N node) {
        this.platform = platform;
        this.id = id;
        this.node = node;
    }

    public SokolPlatform platform() { return platform; }
    @Override public @NotNull String id() { return id; }

    /**
     * Gets the localized name of this blueprint.
     * @param locale The locale to localize for.
     * @return The name.
     */
    public @NotNull net.kyori.adventure.text.Component name(@NotNull Locale locale) {
        return platform.localize(locale, "blueprint." + id);
    }

    public N node() { return node; }

    public N build() { return node.asRoot(); }
}
