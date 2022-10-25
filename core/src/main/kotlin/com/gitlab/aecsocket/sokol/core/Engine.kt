package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.util.*
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
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

const val PRIORITY_EARLIEST = -1000

const val PRIORITY_EARLY = -100

const val PRIORITY_NORMAL = 0

const val PRIORITY_LATE = 100

const val PRIORITY_LATEST = 1000

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

interface ComponentMapper<C : SokolComponent> {
    fun has(entity: SokolComponentAccess): Boolean

    fun mapOr(entity: SokolComponentAccess): C?

    fun map(entity: SokolComponentAccess): C
}

interface SokolComponentAccess {
    fun archetype(): Bits

    fun allComponents(): Set<SokolComponent>

    fun hasComponent(type: Int): Boolean

    fun getComponent(type: Int): SokolComponent?

    fun setComponent(component: SokolComponent)

    fun removeComponent(type: Int)
}

interface SokolEntityBuilder : SokolComponentAccess {
    fun build(): SokolEntityAccess
}

interface SokolEntityAccess : SokolComponentAccess {
    val engine: SokolEngine

    fun <E : SokolEvent> call(event: E): E
}

inline fun <reified C : SokolComponent> SokolEntityAccess.getComponent(): C? = getComponent(engine.componentType<C>()) as? C

data class SokolBlueprint(val components: Collection<SokolComponent>) {
    fun isEmpty() = components.isEmpty()
    fun isNotEmpty() = components.isNotEmpty()

    fun build(engine: SokolEngine): SokolEntityBuilder {
        val entity = engine.entityBuilder()
        components.forEach { entity.setComponent(it) }
        return entity
    }
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
        sortSystems()
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
            override fun has(entity: SokolComponentAccess): Boolean {
                return entity.hasComponent(typeId)
            }

            override fun mapOr(entity: SokolComponentAccess): C? {
                @Suppress("UNCHECKED_CAST")
                return entity.getComponent(typeId) as? C
            }

            override fun map(entity: SokolComponentAccess): C {
                @Suppress("UNCHECKED_CAST")
                return entity.getComponent(typeId) as? C
                    ?: throw IllegalStateException("Entity does not have component $type ($typeId)")
            }
        }
    }

    inline fun <reified C : SokolComponent> componentMapper() = componentMapper<C>(C::class.java)

    fun createArchetype(components: Collection<Class<out SokolComponent>>): Bits {
        return Bits(bitTypesOf(components))
    }

    fun applies(filter: EntityFilter, archetype: Bits): Boolean {
        return archetype.containsAll(filter.all)
            && (filter.one.isEmpty() || filter.one.intersects(archetype))
            && !filter.none.intersects(archetype)
    }
    
    fun entityBuilder(): SokolEntityBuilder = object : BaseComponentAccess(), SokolEntityBuilder {
        override fun build(): SokolEntityAccess {
            val entity = Entity(archetype, components.copyOf())
            entity.call(SokolEvent.Populate)
            return entity
        }
    }

    internal open inner class BaseComponentAccess(
        protected val archetype: Bits = Bits(componentTypes.size),
        protected val components: Array<SokolComponent?> = Array(componentTypes.size) { null }
    ) : SokolComponentAccess {
        override fun archetype() = Bits(archetype)

        override fun allComponents() = components.filterNotNull().toSet()

        override fun hasComponent(type: Int): Boolean {
            return components[type] != null
        }

        override fun getComponent(type: Int) = components[type]

        override fun setComponent(component: SokolComponent) {
            val type = componentType(component.componentType)
            archetype.set(type)
            components[type] = component
        }

        override fun removeComponent(type: Int) {
            components[type] = null
        }
    }
    
    internal inner class Entity(
        archetype: Bits,
        components: Array<SokolComponent?>
    ) : BaseComponentAccess(archetype, components), SokolEntityAccess {
        override val engine get() = this@SokolEngine

        override fun <E : SokolEvent> call(event: E): E {
            val eventType = event::class.java
            systems.forEach { system ->
                if (applies(system.filter, archetype)) {
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
