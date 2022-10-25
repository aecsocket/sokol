package com.gitlab.aecsocket.sokol.core

import java.util.UUID

class SokolSpace(val engine: SokolEngine) {
    private val _entities = HashMap<UUID, SokolEntityAccess>()
    val entities: Map<UUID, SokolEntityAccess> get() = _entities

    operator fun contains(id: UUID) = _entities.contains(id)

    operator fun get(id: UUID) = _entities[id]

    fun add(entity: SokolEntityAccess, id: UUID = UUID.randomUUID()): UUID {
        if (contains(id))
            throw IllegalStateException("Trying to add entity $entity with ID $id, which already exists (${get(id)})")
        entity.call(SokolEvent.Add)
        _entities[id] = entity
        return id
    }

    fun remove(id: UUID): SokolEntityAccess? {
        return _entities.remove(id)?.also {
            it.call(SokolEvent.Remove)
        }
    }

    fun call(event: SokolEvent) {
        _entities.forEach { (_, entity) ->
            entity.call(event)
        }
    }

    fun update() {
        call(SokolEvent.Update)
    }
}