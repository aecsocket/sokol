package com.gitlab.aecsocket.sokol.core.stat

import net.kyori.adventure.key.Key

private fun keyError(key: Key): () -> Nothing = {
    throw NoSuchElementException("No value for stat with key '$key'")
}

interface CompiledStatMap {
    val entries: Map<Key, Stat.Node.First<*>>

    fun <T : Any> node(key: Key): Stat.Node.First<T>?

    fun <T : Any> node(stat: Stat<T>): Stat.Node.First<T>? = node(stat.key)

    fun <T : Any> value(key: Key): T? = node<T>(key)?.compute()

    fun <T : Any> value(stat: Stat<T>): T? = node(stat)?.compute()

    fun <T : Any> valueOr(
        key: Key,
        default: () -> T = keyError(key)
    ): T = node<T>(key)?.compute() ?: default()

    fun <T : Any> valueOr(
        stat: Stat<T>,
        default: () -> T = keyError(stat.key)
    ): T = node(stat)?.compute() ?: default()
}

private object EmptyCompiledStatMap : CompiledStatMap {
    override val entries: Map<Key, Stat.Node.First<*>>
        get() = emptyMap()

    override fun <T : Any> node(key: Key) = null
}

private class CompiledStatMapImpl(
    override val entries: Map<Key, Stat.Node.First<*>>
) : CompiledStatMap {
    override fun <T : Any> node(key: Key) = entries[key]?.let {
        @Suppress("UNCHECKED_CAST")
        it as Stat.Node.First<T>
    }
}

fun emptyCompiledStatMap(): CompiledStatMap = EmptyCompiledStatMap

fun compiledStatMapOf(entries: Map<Key, Stat.Node.First<*>>): CompiledStatMap =
    CompiledStatMapImpl(entries)

interface StatMap {
    val entries: Map<Key, Stat.Node<*>>

    fun <T : Any> node(key: Key): Stat.Node<T>?

    fun <T : Any> nodeOr(
        key: Key,
        default: () -> Stat.Node<T> = keyError(key)
    ) = node(key) ?: default()

    fun <T : Any> node(stat: Stat<T>): Stat.Node<T>? = node(stat.key)

    fun <T : Any> nodeOr(
        stat: Stat<T>,
        default: () -> Stat.Node<T> = keyError(stat.key)
    ) = node(stat) ?: default()

    fun compile(): CompiledStatMap
}

private object EmptyStatMap : StatMap {
    override val entries: Map<Key, Stat.Node<*>>
        get() = emptyMap()

    override fun <T : Any> node(key: Key) = null

    override fun compile() = emptyCompiledStatMap()

    override fun toString() = "Stats{}"
}

interface MutableStatMap : StatMap {
    override val entries: MutableMap<Key, Stat.Node<*>>

    fun <T : Any> set(key: Key, node: Stat.Node<T>)

    fun <T : Any> set(stat: Stat<T>, node: Stat.Node<T>) =
        set(stat.key, node)

    fun <T : Any> merge(key: Key, node: Stat.Node<T>)

    fun <T : Any> merge(stat: Stat<T>, node: Stat.Node<T>) =
        merge(stat.key, node)

    fun merge(other: StatMap)
}

class WrapperStatMap(
    override val entries: MutableMap<Key, Stat.Node<*>>
) : MutableStatMap {
    override fun <T : Any> node(key: Key) = entries[key]?.let {
        @Suppress("UNCHECKED_CAST")
        it as Stat.Node<T>
    }

    override fun <T : Any> set(key: Key, node: Stat.Node<T>) {
        entries[key] = node
    }

    override fun <T : Any> merge(key: Key, node: Stat.Node<T>) {
        val existing = entries[key]
        if (existing == null || node.value is Stat.Value.Discarding) {
            entries[key] = node
        } else {
            @Suppress("UNCHECKED_CAST")
            (existing as Stat.Node<T>).next = node
        }
    }

    override fun merge(other: StatMap) {
        other.entries.forEach { (key, node) -> merge(key, node) }
    }

    override fun compile(): CompiledStatMap {
        return compiledStatMapOf(entries.map { (key, node) ->
            if (node is Stat.Node.First) key to node
            else throw IllegalStateException("Value for stat $key is not a first node")
        }.associate { it })
    }

    override fun toString() = "Stats$entries"
}

fun emptyStatMap(): StatMap = EmptyStatMap

fun statMapOf(entries: MutableMap<Key, Stat.Node<*>> = HashMap()): MutableStatMap = WrapperStatMap(entries)
