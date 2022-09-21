package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.util.*
import java.lang.invoke.MethodHandles
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
    val eventListeners: Map<Class<out SokolEvent>, (SokolEvent, SokolEntityAccess) -> Unit>,
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
    fun mapOr(entity: SokolEntityAccess): C?

    fun map(entity: SokolEntityAccess): C
}

interface SokolEntityAccess {
    val engine: SokolEngine

    fun archetype(): Archetype

    fun allComponents(): Set<SokolComponent>

    fun getComponent(type: Int): SokolComponent?

    fun addComponent(component: SokolComponent)

    fun removeComponent(type: Int)

    fun <E : SokolEvent> call(event: E): E
}

inline fun <reified C : SokolComponent> SokolEntityAccess.component(): C? = getComponent(engine.componentType<C>()) as? C

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
        val eventListeners = HashMap<Class<out SokolEvent>, (SokolEvent, SokolEntityAccess) -> Unit>()

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

                    if (method.parameterCount != 2)
                        error("event listener must have parameters (event, entity)")
                    val eventType = method.parameterTypes[0]
                    if (!SokolEvent::class.java.isAssignableFrom(eventType))
                        error("event type must extend SokolEvent")
                    @Suppress("UNCHECKED_CAST")
                    eventType as Class<out SokolEvent>
                    if (eventListeners.contains(eventType))
                        error("duplicate event listener")

                    val handle = try {
                        handleLookup.unreflect(method)
                    } catch (ex: Exception) {
                        error("could not make handle for listener method", ex)
                    }.bindTo(system)

                    eventListeners[eventType] = { event, entity -> handle.invoke(event, entity) }
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
            override fun mapOr(entity: SokolEntityAccess): C? {
                @Suppress("UNCHECKED_CAST")
                return entity.getComponent(typeId) as? C
            }

            override fun map(entity: SokolEntityAccess): C {
                @Suppress("UNCHECKED_CAST")
                return entity.getComponent(typeId) as? C
                    ?: throw IllegalStateException("Entity does not have component $type ($typeId)")
            }
        }
    }

    inline fun <reified C : SokolComponent> componentMapper() = componentMapper<C>(C::class.java)

    fun createArchetype(components: Collection<Class<out SokolComponent>>): Archetype {
        return Archetype(bitTypesOf(components))
    }

    fun applies(filter: EntityFilter, archetype: Bits): Boolean {
        return archetype.containsAll(filter.all)
            && (filter.one.isEmpty() || filter.one.intersects(archetype))
            && !filter.none.intersects(archetype)
    }

    fun createEntity(archetype: Archetype = Archetype.Empty): SokolEntityAccess {
        return object : SokolEntityAccess {
            val currentArchetype = Bits(archetype.components)
            val components = Array<SokolComponent?>(componentTypes.size) { null }

            override val engine get() = this@SokolEngine

            override fun archetype() = Archetype(currentArchetype)

            override fun allComponents() = components.filterNotNull().toSet()

            override fun getComponent(type: Int) = components[type]

            fun setComponent(type: Int, component: SokolComponent) {
                currentArchetype.set(type)
                components[type] = component
            }

            override fun addComponent(component: SokolComponent) {
                setComponent(componentType(component.componentType), component)
            }

            override fun removeComponent(type: Int) {
                currentArchetype.clear(type)
                components[type] = null
            }

            override fun <E : SokolEvent> call(event: E): E {
                val eventType = event::class.java
                systems.forEach { system ->
                    if (applies(system.filter, currentArchetype)) {
                        system.eventListeners.forEach { (type, listener) ->
                            if (type.isAssignableFrom(eventType)) {
                                listener(event, this)
                            }
                        }
                    }
                }
                return event
            }

            override fun toString(): String {
                return "SokolEntity(${allComponents().joinToString()})"
            }
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
    fun isEmpty() = components.isEmpty()

    fun archetype(engine: SokolEngine) = engine.createArchetype(components.map { it.componentType })

    fun apply(entity: SokolEntityAccess) {
        components.forEach {
            entity.addComponent(it)
        }
    }

    fun create(engine: SokolEngine): SokolEntityAccess {
        if (components.isEmpty())
            throw IllegalArgumentException("Cannot create entity from blueprint with no components")
        val entity = engine.createEntity(archetype(engine))
        apply(entity)
        return entity
    }

    fun byType(type: Class<out SokolComponent>): SokolComponent? {
        return components.find { it.componentType === type }
    }

    inline fun <reified C : SokolComponent> byType(): C? = byType(C::class.java) as? C

    fun containsType(type: Class<out SokolComponent>) = byType(type) != null

    inline fun <reified C : SokolComponent> containsType() = containsType(C::class.java)
}
