package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key

data class EntityFilter(
    val all: Set<String> = emptySet(),
    val one: Set<String> = emptySet(),
    val none: Set<String> = emptySet(),
) {
    fun matches(entity: SokolEntity): Boolean {
        val keys = entity.components.keys
        return keys.containsAll(all)
            && (one.isEmpty() || keys.intersect(one).isNotEmpty())
            && keys.intersect(none).isEmpty()
    }
}

fun entityFilterOf(
    all: Set<Key> = emptySet(),
    one: Set<Key> = emptySet(),
    none: Set<Key> = emptySet()
) = EntityFilter(
    all.map { it.asString() }.toSet(),
    one.map { it.asString() }.toSet(),
    none.map { it.asString() }.toSet(),
)

interface EntityAccessor {
    fun all(): Collection<SokolEntity>

    fun by(filter: EntityFilter): Iterable<SokolEntity>
}

class SimpleEntityAccessor(
    val entities: Collection<SokolEntity>,
) : EntityAccessor {
    override fun all() = entities

    override fun by(filter: EntityFilter): Iterable<SokolEntity> {
        return entities.filter { filter.matches(it) }
    }
}
