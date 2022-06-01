package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.NodeComponent
import com.github.aecsocket.sokol.core.Slot
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val FEATURES = "features"
private const val SLOTS = "slots"

abstract class ComponentSerializer<
    T : NodeComponent.Scoped<T, P, S>,
    F : Feature<*, P>,
    P : Feature.Profile<*>,
    S : Slot
> : TypeSerializer<T> {
    protected abstract fun feature(id: String): F?

    protected abstract fun slot(key: String, node: ConfigurationNode): S

    protected abstract fun create(
        id: String,
        features: Map<String, P>,
        slots: Map<String, S>
    ): T

    override fun serialize(type: Type, obj: T?, node: ConfigurationNode) =
        throw UnsupportedOperationException()

    override fun deserialize(type: Type, node: ConfigurationNode): T {
        val id = try {
            Keyed.validate(node.key().toString())
        } catch (ex: Keyed.ValidationException) {
            throw SerializationException(node, type, "Invalid key")
        }

        val features = HashMap<String, P>()
        node.node(FEATURES).childrenMap().forEach { (key, child) ->
            val featureId = key.toString()
            val feature = feature(featureId)
                ?: throw SerializationException(child, type, "No feature with ID '$featureId'")
            val data = feature.createProfile(child)
            features[featureId] = data
        }

        val slots = HashMap<String, S>()
        node.node(SLOTS).childrenMap().forEach { (key, child) ->
            val slotKey = try {
                Keyed.validate(key.toString())
            } catch (ex: Keyed.ValidationException) {
                throw SerializationException(child, type, "Invalid slot key")
            }
            slots[slotKey] = slot(slotKey, child)
        }

        return create(id, features, slots)
    }
}
