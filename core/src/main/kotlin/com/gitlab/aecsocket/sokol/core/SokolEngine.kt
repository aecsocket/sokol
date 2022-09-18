package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.extension.TPS

const val TIMING_MAX_MEASUREMENTS = 60 * TPS

class SokolEngine {
    private val _systems = HashSet<SokolSystem>()
    val systems: Set<SokolSystem> get() = _systems

    val timings = Timings(TIMING_MAX_MEASUREMENTS)

    fun addSystem(system: SokolSystem) {
        _systems.add(system)
    }

    fun removeSystem(system: SokolSystem) {
        _systems.remove(system)
    }

    fun createEntity(components: Iterable<SokolComponent> = emptySet()): SokolEntity {
        val compsMap = components
            .map { component -> component.key.asString() to component }
            .associate { it }
        return EntityImpl(compsMap.toMutableMap())
    }

    fun call(entities: Collection<SokolEntity>, event: SokolEvent) {
        val accessor = SimpleEntityAccessor(entities)
        _systems.forEach { system ->
            system.handle(accessor, event)
        }
    }

    fun update(entities: Collection<SokolEntity>) {
        timings.time {
            call(entities, UpdateEvent)
        }
    }
}
