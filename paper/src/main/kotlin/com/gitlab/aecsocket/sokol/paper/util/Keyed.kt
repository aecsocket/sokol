package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

fun validateStringKey(type: Type, node: ConfigurationNode): String {
    return try {
        Keyed.validate(node.key().toString())
    } catch (ex: Keyed.ValidationException) {
        throw SerializationException(node, type, "Invalid key", ex)
    }
}

fun validateNamespacedKey(type: Type, node: ConfigurationNode): Key {
    return try {
        Key.key(node.key().toString())
    } catch (ex: InvalidKeyException) {
        throw SerializationException(node, type, "Invalid namespaced key", ex)
    }
}
