package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.util.*

interface SokolComponent {
    val type: ComponentType<*>
}

interface ComponentType<C : SokolComponent>

fun interface SokolSystem {
    fun handle(space: SokolEngine.Space, event: SokolEvent)
}

data class EntityFilter internal constructor(
    val all: Bits,
    val one: Bits,
    val none: Bits,
)

@JvmInline
value class Archetype internal constructor(val components: Bits) {
    companion object {
        val Empty = Archetype(Bits(0))
    }
}

fun interface ComponentMapper<C : SokolComponent> {
    fun map(space: SokolEngine.Space, entity: Int): C
}

class SokolEngine internal constructor(
    private val componentTypes: Map<ComponentType<*>, Int>
) {
    private val _systems = HashSet<SokolSystem>()
    val systems: Set<SokolSystem> get() = _systems

    fun addSystem(system: SokolSystem) {
        _systems.add(system)
    }

    fun removeSystem(system: SokolSystem) {
        _systems.remove(system)
    }

    fun componentType(type: ComponentType<*>): Int {
        return componentTypes[type] ?: throw IllegalStateException("Component type $type is not registered on this engine")
    }

    private fun bitTypesOf(types: Collection<ComponentType<*>>) = Bits(componentTypes.size).apply {
        types.forEach { set(componentType(it)) }
    }

    fun entityFilter(
        all: Collection<ComponentType<*>> = emptySet(),
        one: Collection<ComponentType<*>> = emptySet(),
        none: Collection<ComponentType<*>> = emptySet(),
    ): EntityFilter {
        return EntityFilter(bitTypesOf(all), bitTypesOf(one), bitTypesOf(none))
    }

    fun <C : SokolComponent> componentMapper(type: ComponentType<C>): ComponentMapper<C> {
        val typeId = componentType(type)
        return ComponentMapper { space, entity ->
            @Suppress("UNCHECKED_CAST")
            space.getComponent(entity, typeId) as C
        }
    }

    fun createArchetype(components: Collection<ComponentType<*>>): Archetype {
        return Archetype(bitTypesOf(components))
    }

    fun createSpace(capacity: Int = 64) = Space(capacity)

    inner class Space internal constructor(capacity: Int) {
        val engine get() = this@SokolEngine
        val entities = emptyBag<Bits>(capacity)
        private val components = Array<MutableBag<SokolComponent>>(componentTypes.size) { emptyBag(capacity) }
        private val freeEntities = IntDeque()

        private fun EntityFilter.applies(entity: Int): Boolean {
            val types = entities[entity]
            return types.containsAll(all)
                && (one.isEmpty() || one.intersects(types))
                && !none.intersects(types)
        }

        val entitiesCount: Int get() = entities.size

        fun entitiesBy(filter: EntityFilter): Set<Int> {
            val res = HashSet<Int>()
            repeat(entities.size) { entity ->
                if (filter.applies(entity))
                    res.add(entity)
            }
            return res
        }

        fun createEntity(archetype: Archetype = Archetype.Empty): Int {
            val id = if (freeEntities.isEmpty()) entities.size else freeEntities.popFirst()
            entities[id] = archetype.components
            return id
        }

        fun removeEntity(entity: Int) {
            val archetype = entities[entity]
            archetype.forEachIndexed { index, value ->
                if (value) {
                    components[index].removeAt(entity)
                }
            }
            entities.removeAt(entity)
            freeEntities.add(entity)
        }

        fun clear() {
            entities.clear()
            components.forEach { it.clear() }
            freeEntities.clear()
        }

        fun getComponents(entity: Int): Collection<SokolComponent> {
            val archetype = entities[entity]
            return archetype.mapIndexedNotNull { index, value ->
                if (value) components[index][entity] else null
            }
        }

        fun getArchetype(entity: Int) = Archetype(entities[entity])

        fun getBlueprint(entity: Int): SokolBlueprint {
            val components = HashSet<SokolComponent>()
            entities[entity].forEachIndexed { index, value ->
                if (value) components.add(this.components[index][entity])
            }
            return SokolBlueprint(components)
        }

        fun getComponent(entity: Int, typeId: Int): SokolComponent {
            return components[typeId][entity]
        }

        fun addComponent(entity: Int, component: SokolComponent) {
            val componentType = componentType(component.type)
            components[componentType][entity] = component
            entities[entity].set(componentType)
        }

        fun removeComponent(entity: Int, componentType: Int) {
            components[componentType].removeAt(entity)
            entities[entity].clear(componentType)
        }

        fun <E : SokolEvent> call(event: E): E {
            systems.forEach { system ->
                system.handle(this, event)
            }
            return event
        }
    }

    class Builder {
        fun interface SystemFactory {
            fun create(engine: SokolEngine): SokolSystem
        }

        private val _systemFactories = HashSet<SystemFactory>()
        val systemFactories: Set<SystemFactory> get() = _systemFactories

        private val _componentTypes = HashSet<ComponentType<*>>()
        val componentTypes: Set<ComponentType<*>> get() = _componentTypes

        fun systemFactory(factory: SystemFactory): Builder {
            _systemFactories.add(factory)
            return this
        }

        fun componentType(type: ComponentType<*>): Builder {
            _componentTypes.add(type)
            return this
        }

        fun build() = SokolEngine(
            _componentTypes.mapIndexed { index, type -> type to index }.associate { it }
        ).apply {
            _systemFactories.forEach { factory ->
                addSystem(factory.create(this))
            }
        }
    }
}

class SokolBlueprint(val components: Collection<SokolComponent>) {
    fun archetype(engine: SokolEngine) = engine.createArchetype(components.map { it.type })

    fun apply(space: SokolEngine.Space, entity: Int) {
        components.forEach { component ->
            space.addComponent(entity, component)
        }
    }

    fun create(space: SokolEngine.Space): Int {
        val entity = space.createEntity(archetype(space.engine))
        apply(space, entity)
        return entity
    }

    fun <C : SokolComponent> byType(type: ComponentType<C>): C? {
        @Suppress("UNCHECKED_CAST")
        return components.find { it.type === type } as? C
    }

    fun containsType(type: ComponentType<*>) = byType(type) != null
}
