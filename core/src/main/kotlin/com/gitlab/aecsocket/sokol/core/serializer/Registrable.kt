package com.gitlab.aecsocket.sokol.core.serializer

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.stat.ApplicableStats
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.kotlin.extensions.get
import java.lang.reflect.Type

private const val TAGS = "tags"
private const val FEATURES = "features"
private const val SLOTS = "slots"
private const val STATS = "stats"

abstract class ComponentSerializer<
    T : NodeComponent.Scoped<T, P, S>,
    F : Feature<P>,
    P : Feature.Profile<*>,
    S : Slot
> : TypeSerializer<T> {
    protected abstract fun feature(id: String): F?

    protected abstract fun slot(key: String, node: ConfigurationNode): S

    override fun serialize(type: Type, obj: T?, node: ConfigurationNode) {}

    protected fun id(type: Type, node: ConfigurationNode) = try {
        Keyed.validate(node.key().toString())
    } catch (ex: Keyed.ValidationException) {
        throw SerializationException(node, type, "Invalid key")
    }

    protected fun tags(type: Type, node: ConfigurationNode) =
        node.node(TAGS).get { HashSet<String>() }

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

    protected fun stats(type: Type, node: ConfigurationNode) =
        node.node(STATS).get { ArrayList<ApplicableStats>() }

    protected fun featureStatTypes(features: Iterable<Feature<*>>) = features
        .flatMap { it.statTypes.entries.map { (key, stat) ->
            if (key.namespace() != it.id)
                throw IllegalStateException("Feature '${it.id}' registers stat type keys with namespace '${key.namespace()}'")
            key to stat
        } }
        .associate { (key, stat) -> key.toString() to stat }

    protected fun featureRuleTypes(features: Iterable<Feature<*>>) = features
        .flatMap { it.ruleTypes.entries.map { (key, type) ->
            if (key.namespace() != it.id)
                throw IllegalStateException("Feature '${it.id} registers rule type keys with namespace '${key.namespace()}'")
            key to type
        } }
        .associate { (key, type) -> key.toString() to type }
}

abstract class BlueprintSerializer<
        T : Blueprint<N>,
        N : DataNode
        > : TypeSerializer<T> {
    protected abstract val nodeType: Class<N>

    override fun serialize(type: Type, obj: T?, node: ConfigurationNode) {}

    protected fun id(type: Type, node: ConfigurationNode) = try {
        Keyed.validate(node.key().toString())
    } catch (ex: Keyed.ValidationException) {
        throw SerializationException(node, type, "Invalid key")
    }

    protected fun node(type: Type, node: ConfigurationNode) =
        node.get(nodeType) ?: throw SerializationException(node, type, "Null node")
}
