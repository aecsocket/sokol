package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.NodeComponent
import com.github.aecsocket.sokol.core.Slot
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val FEATURES = "features"
private const val SLOTS = "slots"
private const val TAGS = "tags"

abstract class ComponentSerializer<
    T : NodeComponent.Scoped<T, P, S>,
    F : Feature<P>,
    P : Feature.Profile<*>,
    S : Slot
> : TypeSerializer<T> {
    protected abstract fun feature(id: String): F?

    protected abstract fun slot(key: String, node: ConfigurationNode): S

    override fun serialize(type: Type, obj: T?, node: ConfigurationNode) =
        throw UnsupportedOperationException()

    protected fun id(type: Type, node: ConfigurationNode) = try {
        Keyed.validate(node.key().toString())
    } catch (ex: Keyed.ValidationException) {
        throw SerializationException(node, type, "Invalid key")
    }

    protected fun features(type: Type, node: ConfigurationNode) =
        node.node(FEATURES).childrenMap().map { (key, child) ->
            val featureId = key.toString()
            val feature = feature(featureId)
                ?: throw SerializationException(child, type, "No feature with ID '$featureId'")
            featureId to feature.createProfile(child)
        }.associate { it }

    protected fun slots(type: Type, node: ConfigurationNode) =
        node.node(SLOTS).childrenMap().map { (key, child) ->
            val slotKey = try {
                Keyed.validate(key.toString())
            } catch (ex: Keyed.ValidationException) {
                throw SerializationException(child, type, "Invalid slot key")
            }
            slotKey to slot(slotKey, child)
        }.associate { it }

    protected fun tags(type: Type, node: ConfigurationNode) =
        node.node(TAGS).get<MutableSet<String>> { HashSet() }
}
