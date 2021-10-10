package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.ValidationException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Objects;

/* package */ final class Utils {
    private Utils() {}

    public static String id(Type type, ConfigurationNode node) throws SerializationException {
        String id = Objects.requireNonNull(node.key()).toString();
        try {
            Keyed.validate(id);
        } catch (ValidationException e) {
            throw new SerializationException(node, type, "Invalid ID '" + id + "'", e);
        }
        return id;
    }
}
