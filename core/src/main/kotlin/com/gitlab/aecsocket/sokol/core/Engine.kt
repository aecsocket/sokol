package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.util.*
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.reflect.KClass

interface SokolComponent {
    val componentType: Class<out SokolComponent>
}

interface SokolSystem {
    fun interface Factory {
        fun create(engine: SokolEngine): SystemDefinition
    }
}

@Target(AnnotationTarget.CLASS)
annotation class All(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class One(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class None(vararg val types: KClass<out SokolComponent>)

@Target(AnnotationTarget.CLASS)
annotation class Priority(val value: Int)

@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe

data class SystemDefinition internal constructor(
    val filter: EntityFilter,
    val priority: Int,
    val eventListeners: Map<Class<out SokolEvent>, (SokolEvent, SokolEngine.Space, Int) -> Unit>,
    val system: SokolSystem,
)

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

interface ComponentMapper<C : SokolComponent> {
    fun mapOr(space: SokolEngine.Space, entity: Int): C?

    fun map(space: SokolEngine.Space, entity: Int): C
}

class SokolEngine internal constructor(
    private val componentTypes: Map<Class<out SokolComponent>, Int>
) {
    private val _systems = ArrayList<SystemDefinition>()
    val systems: List<SystemDefinition> get() = _systems

    private val handleLookup = MethodHandles.publicLookup()

    private fun sortSystems() {
        _systems.sortBy { it.priority }
    }

    fun define(system: SokolSystem): SystemDefinition {
        var all = emptyList<Class<out SokolComponent>>()
        var one = emptyList<Class<out SokolComponent>>()
        var none = emptyList<Class<out SokolComponent>>()
        var priority = 0
        val eventListeners = HashMap<Class<out SokolEvent>, (SokolEvent, Space, Int) -> Unit>()

        val systemType = system::class
        systemType.annotations.forEach { annotation ->
            if (annotation is All)
                all = annotation.types.map { it.java }
            if (annotation is One)
                one = annotation.types.map { it.java }
            if (annotation is None)
                none = annotation.types.map { it.java }
            if (annotation is Priority)
                priority = annotation.value
        }
        systemType.java.methods.forEach { method ->
            method.declaredAnnotations.forEach { annotation ->
                if (annotation is Subscribe) {
                    val methodName = method.name
                    fun error(message: String, cause: Throwable? = null): Nothing =
                        throw IllegalArgumentException("${systemType.qualifiedName}.${methodName}(${method.parameterTypes.joinToString { it.simpleName }}): ${method.returnType}: $message", cause)

                    if (method.parameterCount != 3)
                        error("event listener must have parameters (event, space, entity)")
                    val eventType = method.parameterTypes[0]
                    if (!SokolEvent::class.java.isAssignableFrom(eventType))
                        error("event type must extend SokolEvent")
                    @Suppress("UNCHECKED_CAST")
                    eventType as Class<out SokolEvent>

                    if (eventListeners.contains(eventType))
                        error("duplicate event listener")

                    val methodType = MethodType.methodType(Void.TYPE, eventType, Space::class.java, Int::class.java)
                    val handle = try {
                        handleLookup.findVirtual(systemType.java, methodName, methodType)
                    } catch (ex: Exception) {
                        error("could not make handle for listener method", ex)
                    }

                    //handle.bindTo(system)

                    eventListeners[eventType] = { event, space, entity -> handle.invoke(system, event, space, entity) }
                }
            }
        }

        return SystemDefinition(entityFilter(all, one, none), priority, eventListeners, system)
    }

    fun addSystem(system: SystemDefinition) {
        _systems.add(system)
        sortSystems()
    }

    fun addSystems(systems: Iterable<SystemDefinition>) {
        _systems.addAll(systems)
        sortSystems()
    }

    fun addSystems(vararg systems: SystemDefinition) {
        systems.forEach { _systems.add(it) }
    }

    fun removeSystem(system: SystemDefinition) {
        _systems.remove(system)
    }

    fun removeSystems(systems: Iterable<SystemDefinition>) {
        _systems.removeAll(systems.toSet())
    }

    fun removeSystems(vararg systems: SystemDefinition) {
        systems.forEach { _systems.remove(it) }
    }

    fun componentType(type: Class<out SokolComponent>): Int {
        return componentTypes[type] ?: throw IllegalStateException("Component type $type is not registered on this engine")
    }

    inline fun <reified C : SokolComponent> componentType() = componentType(C::class.java)

    private fun bitTypesOf(types: Collection<Class<out SokolComponent>>) = Bits(componentTypes.size).apply {
        types.forEach { set(componentType(it)) }
    }

    fun entityFilter(
        all: Collection<Class<out SokolComponent>> = emptySet(),
        one: Collection<Class<out SokolComponent>> = emptySet(),
        none: Collection<Class<out SokolComponent>> = emptySet(),
    ): EntityFilter {
        return EntityFilter(bitTypesOf(all), bitTypesOf(one), bitTypesOf(none))
    }

    fun <C : SokolComponent> componentMapper(type: Class<out SokolComponent>): ComponentMapper<C> {
        val typeId = componentType(type)
        return object : ComponentMapper<C> {
            override fun mapOr(space: Space, entity: Int): C? {
                @Suppress("UNCHECKED_CAST")
                return space.getComponentOr(entity, typeId) as? C
            }

            override fun map(space: Space, entity: Int): C {
                @Suppress("UNCHECKED_CAST")
                return space.getComponent(entity, typeId) as? C
                    ?: throw IllegalStateException("Entity $entity does not have component $type ($typeId)")
            }
        }
    }

    inline fun <reified C : SokolComponent> componentMapper() = componentMapper<C>(C::class.java)

    fun createArchetype(components: Collection<Class<out SokolComponent>>): Archetype {
        return Archetype(bitTypesOf(components))
    }

    fun createSpace(capacity: Int = 64) = Space(capacity)

    inner class Space internal constructor(capacity: Int) {
        val engine get() = this@SokolEngine
        private val entities = emptyBag<Bits>(capacity)
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

        fun getComponentOr(entity: Int, typeId: Int): SokolComponent? {
            return components[typeId].getOr(entity)
        }

        fun getComponent(entity: Int, typeId: Int): SokolComponent {
            return components[typeId][entity]
        }

        fun addComponent(entity: Int, component: SokolComponent) {
            val componentType = componentType(component.componentType)
            components[componentType][entity] = component
            entities[entity].set(componentType)
        }

        fun removeComponent(entity: Int, componentType: Int) {
            components[componentType].removeAt(entity)
            entities[entity].clear(componentType)
        }

        fun <E : SokolEvent> call(event: E): E {
            val eventType = event::class.java
            systems.forEach { system ->
                system.eventListeners.forEach { (type, listener) ->
                    if (type.isAssignableFrom(eventType)) {
                        entitiesBy(system.filter).forEach { entity ->
                            listener(event, this, entity)
                        }
                    }
                }
            }
            return event
        }
    }

    class Builder {
        private val _systemFactories = HashSet<SokolSystem.Factory>()
        val systemFactories: Set<SokolSystem.Factory> get() = _systemFactories

        private val _componentTypes = HashSet<Class<out SokolComponent>>()
        val componentTypes: Set<Class<out SokolComponent>> get() = _componentTypes

        fun systemFactory(factory: SokolSystem.Factory): Builder {
            _systemFactories.add(factory)
            return this
        }

        fun componentType(type: Class<out SokolComponent>): Builder {
            _componentTypes.add(type)
            return this
        }

        inline fun <reified C : SokolComponent> componentType() = componentType(C::class.java)

        fun build() = SokolEngine(
            _componentTypes.mapIndexed { index, type -> type to index }.associate { it }
        ).apply {
            addSystems(_systemFactories.map { it.create(this) })
        }
    }
}

class SokolBlueprint(val components: Collection<SokolComponent>) {
    fun archetype(engine: SokolEngine) = engine.createArchetype(components.map { it.componentType })

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

    fun byType(type: Class<out SokolComponent>): SokolComponent? {
        return components.find { it.componentType === type }
    }

    inline fun <reified C : SokolComponent> byType(): C? = byType(C::class.java) as? C

    fun containsType(type: Class<out SokolComponent>) = byType(type) != null

    inline fun <reified C : SokolComponent> containsType() = containsType(C::class.java)
}
