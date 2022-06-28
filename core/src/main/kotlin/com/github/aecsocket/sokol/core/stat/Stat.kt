package com.github.aecsocket.sokol.core.stat

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

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

        class First<T : Any>(
            stat: Stat<T>,
            override val value: Value.First<T>,
            next: Node<T>? = null
        ) : Node<T>(stat, value, next) {
            fun compute(): T {
                val res = value.first()
                return next?.value(res) ?: res
            }
        }
    }
}

abstract class AbstractStat<T : Any>(
    namespace: String, key: String
) : Stat<T> {
    override val key = Key.key(namespace, key)
}

private fun <T : Any> statNodeOfInternal(stat: Stat<T>, values: Collection<Stat.Value<T>>): Stat.Node<T>? {
    return if (values.isEmpty()) null
    else Stat.Node(stat, values.first(), statNodeOfInternal(stat, values.drop(1)))
}

fun <T : Any> statNodeOf(stat: Stat<T>, values: Collection<Stat.Value<T>>): Stat.Node<T> {
    return statNodeOfInternal(stat, values) ?: throw IllegalArgumentException("No stat values passed")
}

fun statTypes(vararg stats: Stat<*>): Map<Key, Stat<*>> = stats.map {
    it.key to it
}.associate { it }
