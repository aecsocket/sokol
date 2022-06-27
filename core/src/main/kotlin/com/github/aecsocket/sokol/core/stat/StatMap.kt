package com.github.aecsocket.sokol.core.stat

import net.kyori.adventure.key.Key

interface StatMap {
    val entries: Map<Key, Stat<*>>

    fun <T : Any> get(key: Key, default: () -> T = { throw NoSuchElementException() }): T

    fun <T : Any> get(stat: Stat<T>, default: () -> T = { throw NoSuchElementException() }) =
        get(stat.key, default)
}

interface MutableStatMap : StatMap {

}

class HashStatMap(
    override val entries: Map<Key, Stat<*>> = HashMap()
) : MutableStatMap {
    override fun <T : Any> get(key: Key, default: () -> T) = entries[key]?.let {
        @Suppress("UNCHECKED_CAST")
        it as T
    } ?: default()
}
