package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key

const val TIMING_MAX_MEASUREMENTS = 20 * 60

data class SokolEntityFilter(
    val all: Set<Key> = emptySet(),
) {
    fun matches(entity: SokolEntity): Boolean {
        val keys = entity.components.keys
        return keys.containsAll(all)
    }
}

class SokolEngine {
    private val _entities = HashSet<SokolEntity>()
    val entities: Set<SokolEntity> get() = _entities

    private val _systems = HashSet<SokolSystem>()
    val systems: Set<SokolSystem> get() = _systems

    val timings = Timings(TIMING_MAX_MEASUREMENTS)

    var updating = false
        private set

    fun addEntity(entity: SokolEntity) {
        _entities.add(entity)
    }

    fun removeEntity(entity: SokolEntity) {
        _entities.remove(entity)
    }

    fun clearEntities() {
        _entities.clear()
    }

    fun addSystem(system: SokolSystem) {
        _systems.add(system)
    }

    fun removeSystem(system: SokolSystem) {
        _systems.remove(system)
    }

    fun entitiesFor(filter: SokolEntityFilter, callback: (SokolEntity) -> Unit) {
        _entities.forEach { entity ->
            if (filter.matches(entity))
                callback(entity)
        }
    }

    fun entitiesFor(filter: SokolEntityFilter): Set<SokolEntity> {
        return HashSet<SokolEntity>().apply { entitiesFor(filter) { add(it) } }
    }

    fun update() {
        if (updating) return
        updating = true

        timings.time {
            _systems.forEach { it.update() }
        }

        updating = false
    }
}