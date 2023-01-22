package com.gitlab.aecsocket.sokol.core

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KProperty

class Delta<T>(var value: T, dirty: Boolean = false) {
    var dirty = dirty
        private set

    fun set(value: T) {
        this.value = value
        dirty = true
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }

    fun dirty(): Delta<T> {
        dirty = true
        return this
    }

    fun clean(): Delta<T> {
        dirty = false
        return this
    }

    fun <R> ifDirty(consumer: (T) -> R): R? {
        return if (dirty) consumer(value) else null
    }

    override fun toString() = "($value)${if (dirty) "*" else ""}"
}

object DeltaSerializer : TypeSerializer<Delta<*>> {
    override fun serialize(type: Type, obj: Delta<*>?, node: ConfigurationNode) {
        if (type !is ParameterizedType)
            throw SerializationException(node, type, "Raw types are not supported for deltas")
        if (type.actualTypeArguments.size != 1)
            throw SerializationException(node, type, "Delta expected one type argument")

        val valueType = type.actualTypeArguments[0]
        val valueSerial = node.options().serializers()[valueType]
            ?: throw SerializationException(node, type, "No type serializer available for value type $valueType")

        if (obj == null) node.set(null)
        else {
            valueSerial.serialize(valueType, obj.value as Nothing?, node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): Delta<*> {
        if (type !is ParameterizedType)
            throw SerializationException(node, type, "Raw types are not supported for deltas")
        if (type.actualTypeArguments.size != 1)
            throw SerializationException(node, type, "Delta expected one type argument")

        val valueType = type.actualTypeArguments[0]
        val valueSerial = node.options().serializers()[valueType]
            ?: throw SerializationException(node, type, "No type serializer available for value type $valueType")

        return Delta(valueSerial.deserialize(valueType, node))
    }
}
