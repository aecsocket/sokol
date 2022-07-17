package com.gitlab.aecsocket.sokol.core.stat

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.forceList
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

interface Stat<T : Any> {
    val key: Key

    fun deserialize(node: ConfigurationNode): Value<T>

    fun interface Value<T : Any> {
        fun next(last: T): T

        interface First<T : Any> : Value<T> {
            fun first(): T
        }

        interface Discarding<T : Any> : First<T> {
            override fun next(last: T) = first()
        }
    }

    open class Node<T : Any>(
        val stat: Stat<T>,
        open val value: Value<T>,
        var next: Node<T>? = null
    ) {
        fun value(last: T): T {
            val res = value.next(last)
            return next?.value(res) ?: res
        }

        fun last(): Node<T> = next?.last() ?: this

        fun add(node: Node<T>) {
            last().next = node
        }

        override fun toString() = next?.let { "$value -> ${it.value}" } ?: value.toString()

        class First<T : Any>(
            stat: Stat<T>,
            override val value: Value.First<T>,
            next: Node<T>? = null
        ) : Node<T>(stat, value, next) {
            fun compute(): T {
                val res = value.first()
                return next?.value(res) ?: res
            }

            override fun toString() = "First(${super.toString()})"
        }
    }
}

abstract class AbstractStat<T : Any>(
    namespace: String, key: String
) : Stat<T> {
    override val key = Key.key(namespace, key)
}


private fun <T : Any> statNodeOfInternal(stat: Stat<T>, values: Collection<Stat.Value<T>>): Stat.Node<T>? {
    if (values.isEmpty()) return null
    val first = values.first()
    return if (first is Stat.Value.First) Stat.Node.First(stat, first, statNodeOfInternal(stat, values.drop(1)))
    else Stat.Node(stat, first, statNodeOfInternal(stat, values.drop(1)))
}

fun <T : Any> statNodeOf(stat: Stat<T>, values: Collection<Stat.Value<T>>): Stat.Node<T> {
    return statNodeOfInternal(stat, values) ?: throw IllegalArgumentException("No stat values passed")
}

fun statTypes(vararg stats: Stat<*>): Map<Key, Stat<*>> = stats.map {
    it.key to it
}.associate { it }

data class Operation<T : Any>(
    val argNames: List<String>,
    val action: (List<ConfigurationNode>) -> Stat.Value<T>
)

inline fun <reified T : Any> opDeserialize(
    node: ConfigurationNode,
    vararg operations: Pair<String, (List<ConfigurationNode>) -> Stat.Value<T>>,
): Stat.Value<T> {
    val type = T::class.java
    val list = node.forceList(type)
    if (list.isEmpty())
        throw SerializationException(node, type, "List must contain at least the operator")
    val opKey = list[0].force<String>()
    val opMap = operations.associate { it }
    val operation = opMap[opKey]
        ?: throw SerializationException(node, type, "Invalid operator '$opKey' for stat type $type")
    val args = list.drop(1)
    return operation(args)
}
