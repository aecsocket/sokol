package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.sokol.core.SokolEntity
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

fun interface EntityCallbackAction {
    fun run(entity: SokolEntity, player: Player): Boolean
}

data class EntityCallback(
    val key: Key,
    val action: EntityCallbackAction
)

class EntityCallbackSerializer(private val entityCallbacks: EntityCallbacks) : TypeSerializer<EntityCallback> {
    override fun serialize(type: Type, obj: EntityCallback?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): EntityCallback {
        val key = node.force<Key>()
        return entityCallbacks.callback(key)
            ?: throw SerializationException(node, type, "Invalid entity callback '$key'")
    }
}

class EntityCallbacks {
    private val _callbacks = HashMap<Key, EntityCallback>()
    val callbacks: Map<Key, EntityCallback> get() = _callbacks

    fun callback(key: Key) = _callbacks[key]

    fun callback(key: Key, action: EntityCallbackAction) {
        if (_callbacks.contains(key))
            throw IllegalArgumentException("Duplicate entity callback $key")
        _callbacks[key] = EntityCallback(key, action)
    }
}
